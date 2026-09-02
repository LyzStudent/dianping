package com.dianping.shop.controller;

import com.dianping.common.dto.Result;
import com.dianping.shop.service.BannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/banner")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    public Result list(){
        return Result.ok(bannerService.queryBannerList());
    }
}
