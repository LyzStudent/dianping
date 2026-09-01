package com.dianping.shop.controller;

import com.dianping.common.dto.Result;
import com.dianping.shop.service.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/favorite")
public class UserFavoriteController {

    @Autowired
    private UserFavoriteService userFavoriteService;

    @PutMapping("/{shopId}")
    public Result toggleFavorite(@PathVariable("shopId") Long shopId){
        return Result.ok(userFavoriteService.toggle(shopId));
    }

    @GetMapping("/status/{shopId}")
    public Result favoriteStatus(@PathVariable("shopId") Long shopId){
        return Result.ok(userFavoriteService.isFavorite(shopId));
    }

    @GetMapping("/count/{shopId}")
    public Result favoriteCount(@PathVariable("shopId") Long shopId){
        return Result.ok(userFavoriteService.favoriteCount(shopId));
    }

    @GetMapping("/list")
    public Result myFavorite(@RequestParam(value = "current",defaultValue = "1") Integer current){
        return Result.ok(userFavoriteService.myFavorites(current));
    }
}
