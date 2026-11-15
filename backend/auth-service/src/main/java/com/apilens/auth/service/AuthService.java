package com.apilens.auth.service;

import com.apilens.auth.domain.EmailChangeHistory;
import com.apilens.auth.domain.Organization;
import com.apilens.auth.domain.User;
import com.apilens.auth.exception.RateLimitExceededException;
import com.apilens.auth.repository.EmailChangeHistoryRepository;
import com.apilens.auth.repository.OrganizationRepository;
import com.apilens.auth.repository.UserRepository;
import com.apilens.auth.service.EmailChangeRateLimitService.RateLimitStatus;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final EmailChangeHistoryRepository emailHistoryRepo;
    private final EmailChangeRateLimitService rateLimitService;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository userRepo, OrganizationRepository orgRepo,
                       EmailChangeHistoryRepository emailHistoryRepo,
                       EmailChangeRateLimitService rateLimitService,
                       PasswordEncoder encoder, JwtService jwt) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.emailHistoryRepo = emailHistoryRepo;
        this.rateLimitService = rateLimitService;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record AuthResult(String accessToken, long expiresIn, UUID userId,
                             UUID orgId, String email, String name, String role) {}

    public record RegisterReq(String email, String password, String name, String orgName) {}

    public record EmailChangeResult(String oldEmail, String newEmail, long retryAfterSeconds) {}

    @Transactional
    public EmailChangeResult changeEmail(UUID userId, String rawNewEmail,
                                         String ipAddress, String userAgent) {
        RateLimitStatus status = rateLimitService.check(userId);
        if (!status.allowed()) {
            throw new RateLimitExceededException(
                    "You have reached the email change limit. Try again later.",
                    status.resetInSeconds());
        }

        User user = loadUser(userId);

        String newEmail = rawNewEmail == null ? "" : rawNewEmail.trim().toLowerCase(Locale.ROOT);
        if (newEmail.isEmpty()) {
            throw new IllegalArgumentException("New email is required");
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from current email");
        }
        if (userRepo.existsByEmailAndDeletedAtIsNull(newEmail)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);

        emailHistoryRepo.save(new EmailChangeHistory(
                UUID.randomUUID(),
                user.getId(),
                oldEmail,
                newEmail,
                Instant.now(),
                truncate(ipAddress, 45),
                truncate(userAgent, 500)
        ));

        rateLimitService.increment(userId);

        return new EmailChangeResult(oldEmail, newEmail, rateLimitService.windowSeconds());
    }

    @Transactional
    public AuthResult register(RegisterReq req) {
        if (userRepo.existsByEmailAndDeletedAtIsNull(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String orgName = req.orgName() != null ? req.orgName() : req.name() + "'s Org";
        Organization org = new Organization(UUID.randomUUID(), orgName, slugify(orgName));
        orgRepo.save(org);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setName(req.name());
        user.setOrgId(org.getId());
        user.setRole("OWNER");
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);

        return toResult(user);
    }

    @Transactional
    public AuthResult login(String email, String password) {
        User user = userRepo.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return toResult(user);
    }

    public User loadUser(UUID userId) {
        return userRepo.findById(userId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public record BulkCreateRequest(int count, String emailPrefix, String emailDomain, String password) {}
    public record BulkCreateResult(int created, List<String> emails) {}

    @Transactional
    public BulkCreateResult bulkCreate(BulkCreateRequest req) {
        int created = 0;
        List<String> createdEmails = new ArrayList<>();

        for (int i = 0; i < req.count(); i++) {
            String email = req.emailPrefix() + i + "@" + req.emailDomain();
            if (userRepo.existsByEmailAndDeletedAtIsNull(email)) {
                continue;
            }

            String orgName = req.emailPrefix() + i + "'s Org";
            Organization org = new Organization(
                    UUID.randomUUID(),
                    orgName,
                    slugify(orgName));
            orgRepo.save(org);

            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordHash(encoder.encode(req.password()));
            user.setName(req.emailPrefix() + " User " + i);
            user.setOrgId(org.getId());
            user.setRole("MEMBER");
            user.setEmailVerified(true);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
            userRepo.save(user);

            createdEmails.add(email);
            created++;
        }

        return new BulkCreateResult(created, createdEmails);
    }

    private AuthResult toResult(User user) {
        String token = jwt.generateAccessToken(user.getId(), user.getOrgId(), user.getRole());
        return new AuthResult(token, jwt.getAccessTtlSeconds(),
                user.getId(), user.getOrgId(), user.getEmail(),
                user.getName(), user.getRole());
    }

    private static String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "") + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
