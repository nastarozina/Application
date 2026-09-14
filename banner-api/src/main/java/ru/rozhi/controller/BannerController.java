package ru.rozhi.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.service.BannerService;

import java.net.URI;
import java.util.List;

@RestController
public class BannerController {

    private static final String BANNER_PATH = "/{id}";

    @Autowired
    private BannerService bannerService;

    @GetMapping(BANNER_PATH)
    public ResponseEntity<@NonNull BannerResponse> getBannerById(@PathVariable String id) {
        BannerResponse banner = bannerService.getBannerById(id);
        if (banner == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(banner);
    }

    @GetMapping
    public List<BannerResponse> getAllBanners() {
        return bannerService.getAllBanners();
    }

    @PostMapping
    public ResponseEntity<@NonNull BannerResponse> createBanner(@RequestBody BannerRequest banner) {
        BannerResponse createdBanner = bannerService.createBanner(banner);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(BANNER_PATH)
                .buildAndExpand(createdBanner.id())
                .toUri();

        return ResponseEntity.created(location).body(createdBanner);
    }

    @PutMapping(BANNER_PATH)
    public ResponseEntity<@NonNull BannerResponse> updateBanner(@PathVariable String id, @RequestBody BannerRequest bannerToUpdate) {
        BannerResponse banner = bannerService.updateBanner(id, bannerToUpdate);

        if (banner == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(banner);
    }
}
