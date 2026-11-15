package com.apilens.auth.controller;

import com.apilens.auth.domain.Organization;
import com.apilens.auth.domain.User;
import com.apilens.auth.repository.OrganizationRepository;
import com.apilens.auth.service.AuthService;
import com.apilens.auth.service.AuthService.AuthResult;
import com.apilens.auth.service.AuthService.EmailChangeResult;
import com.apilens.auth.service.AuthService.RegisterReq;
import com.apilens.auth.service.EmailChangeRateLimitService;
import com.apilens.auth.service.EmailChangeRateLimitService.RateLimitStatus;
import com.apilens.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final EmailChangeRateLimitService rateLimitService;
    private final OrganizationRepository orgRepo;

    public AuthController(AuthService authService, JwtService jwtService,
                          EmailChangeRateLimitService rateLimitService,
                          OrganizationRepository orgRepo) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
        this.orgRepo = orgRepo;
    }

    public record RegisterBody(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String name,
            String orgName) {}

    public record LoginBody(@Email @NotBlank String email, @NotBlank String password) {}

    public record ChangeEmailBody(@Email @NotBlank String newEmail) {}

    @PostMapping("/register")
    public ResponseEntity<AuthResult> register(@Valid @RequestBody RegisterBody body) {
        AuthResult result = authService.register(
                new RegisterReq(body.email(), body.password(), body.name(), body.orgName()));
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResult> login(@Valid @RequestBody LoginBody body) {
        return ResponseEntity.ok(authService.login(body.email(), body.password()));
    }

    @PostMapping("/token")
    public ResponseEntity<AuthResult> token(@Valid @RequestBody LoginBody body) {
        // Same as login but intended for load tests / CI (no refresh cookie)
        return ResponseEntity.ok(authService.login(body.email(), body.password()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing bearer token"));
        }
        Claims claims = jwtService.parse(authHeader.substring(7));
        UUID userId = UUID.fromString(claims.getSubject());
        User user = authService.loadUser(userId);
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "orgId", user.getOrgId(),
                "role", user.getRole()
        ));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        UUID userId = requireUserId(authHeader);
        User user = authService.loadUser(userId);
        RateLimitStatus status = rateLimitService.check(userId);
        String orgName = orgRepo.findById(user.getOrgId())
                .map(Organization::getName)
                .orElse("");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("email", user.getEmail());
        body.put("name", user.getName());
        body.put("orgId", user.getOrgId());
        body.put("orgName", orgName);
        body.put("role", user.getRole());
        body.put("createdAt", user.getCreatedAt());
        body.put("emailChange", rateStatus(status));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChangeEmailBody body,
            HttpServletRequest request) {
        UUID userId = requireUserId(authHeader);
        EmailChangeResult result = authService.changeEmail(
                userId, body.newEmail(), clientIp(request), request.getHeader("User-Agent"));

        return ResponseEntity.ok(Map.of(
                "message", "Email updated successfully",
                "oldEmail", result.oldEmail(),
                "newEmail", result.newEmail(),
                "emailChange", rateStatus(rateLimitService.check(userId))
        ));
    }

    public record BulkCreateBody(
            @Min(1) @Max(100) int count,
            @NotBlank String emailPrefix,
            @NotBlank String emailDomain,
            @NotBlank @Size(min = 8) String password) {}

    @PostMapping("/admin/users/bulk")
    public ResponseEntity<?> bulkCreate(@Valid @RequestBody BulkCreateBody body) {
        AuthService.BulkCreateResult result = authService.bulkCreate(
                new AuthService.BulkCreateRequest(
                        body.count(), body.emailPrefix(), body.emailDomain(), body.password()));

        return ResponseEntity.ok(Map.of(
                "created", result.created(),
                "emails", result.emails()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }

    private UUID requireUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new JwtException("Missing bearer token");
        }
        return UUID.fromString(jwtService.parse(authHeader.substring(7)).getSubject());
    }

    private static Map<String, Object> rateStatus(RateLimitStatus status) {
        return Map.of(
                "allowed", status.allowed(),
                "currentCount", status.currentCount(),
                "maxChanges", status.maxChanges(),
                "resetInSeconds", status.resetInSeconds()
        );
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
