package com.dianping.shop.controller;

import com.dianping.common.dto.Result;
import com.dianping.shop.entity.Shop;
import com.dianping.shop.service.ShopSearchService;
import com.dianping.shop.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;
    @Autowired
    private ShopSearchService shopSearchService;

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
        //同步索引
        shopSearchService.indexById(shop.getId());
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
        shopService.update(shop);
        shopSearchService.indexById(shop.getId());
        return Result.ok();
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

    /**
     * ES 关键词搜索
     * @param keyword
     * @param typeId
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/search")
    public Result search(@RequestParam(value = "keyword", required = false) String keyword,
                         @RequestParam(value = "typeId", required = false) Long typeId,
                         @RequestParam(value = "page", defaultValue = "1") Integer page,
                         @RequestParam(value = "size", defaultValue = "10") Integer size){
        return shopSearchService.searhc(keyword,typeId,page,size);
    }

    /**
     * 全量重建索引
     * @return
     */
    @GetMapping("/search/init")
    public Result initIndex(){
        shopSearchService.syncAll();
        return Result.ok();
    }

}
