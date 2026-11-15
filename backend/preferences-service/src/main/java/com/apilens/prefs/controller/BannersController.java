package com.apilens.prefs.controller;

import com.apilens.prefs.domain.Banner;
import com.apilens.prefs.repository.BannerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/banners")
public class BannersController {

    private final BannerRepository repo;

    public BannersController(BannerRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/active")
    public ResponseEntity<List<Banner>> active() {
        return ResponseEntity.ok(repo.findByActiveTrueOrderByPriorityDesc());
    }
}
