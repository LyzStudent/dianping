package com.dianping.controller;

import com.dianping.dto.Result;
import com.dianping.service.ShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {

    @Autowired
    private ShopTypeService shopTypeService;

    @GetMapping("/list")
    public Result queryTypeList(){
        return shopTypeService.queryList();
    }
}
