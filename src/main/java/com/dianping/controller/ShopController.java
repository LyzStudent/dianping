package com.dianping.controller;


import com.dianping.dto.Result;
import com.dianping.entity.Shop;
import com.dianping.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /**
     * 根据id查询商铺信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result queryById(@PathVariable("id") Long id){
        return shopService.queryById(id);
    }

    /**
     * 新增商铺信息
     * @param shop
     * @return
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop){
        //写入数据库
        shopService.save(shop);
        //返回店铺id
        return Result.ok(shop.getId());
    }

    /**
     * 更新商铺信息
     * @param shop
     * @return
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop){
        return shopService.update(shop);
    }


    /**
     * 根据商铺类型分页查询商铺信息
     * @param typeId
     * @param current
     * @return
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current",defaultValue = "1") Integer current,
            @RequestParam(value = "x",required = false) Double x,
            @RequestParam(value = "y",required = false) Double y
    ){
        return shopService.queryShopByTyoe(typeId,current,x,y);
    }

}
