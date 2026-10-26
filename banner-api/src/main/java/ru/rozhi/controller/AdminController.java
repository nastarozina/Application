package ru.rozhi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.service.BannerService;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final String BANNER_PATH = "/{id}";

    @Autowired
    private BannerService bannerService;

    @PutMapping(BANNER_PATH)
    public BannerResponse updateBanner(@PathVariable String id, @RequestBody BannerRequest bannerToUpdate) {
        return bannerService.updateBanner(id, bannerToUpdate);
    }

    @DeleteMapping(BANNER_PATH)
    public void deleteBanner(@PathVariable String id) {
        bannerService.deleteBanner(id);
    }
}