package com.dianping.shop.controller;

import com.dianping.common.annotation.RequireRole;
import com.dianping.common.dto.Result;
import com.dianping.common.util.UserHolder;
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
        shop.setStatus(0);//强制待审核，禁止匿名自发布
        shop.setUserId(null);//归属不由调用方指定

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
        return shopService.queryShopByType(typeId,current,x,y);
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
        return shopSearchService.search(keyword,typeId,page,size);
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

    /**
     * 商家:创建/更新自己的商铺
     */
    @PostMapping("/merchant/shop")
    @RequireRole("2")
    public Result merchantSaveShop(Shop shop){
        shopService.merchantSaveShop(shop);
        shopSearchService.indexById(shop.getId());//新建/更新后同步索引
        return Result.ok(shop.getId());
    }

    /**
     * 查自己名下的店铺
     */
    @GetMapping("/merchant/shop/mine")
    @RequireRole("2")
    public Result myShops(){
        Long userId=UserHolder.getUser().getId();
        return Result.ok(shopService.query().eq("user_id",userId).list());
    }

    @GetMapping("/admin/shop/list")
    @RequireRole("3")
    public Result adminShopList(@RequestParam(defaultValue = "0") Integer status){
        return Result.ok(shopService.query().eq("status",status).list());
    }

    @PostMapping("/admin/shop/{id}/audit")
    @RequireRole("3")
    public Result auditShop(@PathVariable("id") Long shopId,@RequestParam Integer status){
        Shop shop=shopService.getById(shopId);
        if(shop==null){
            return Result.fail("店铺不存在");
        }
        boolean ok=shopService.update().setSql("status="+status).eq("id",shopId).update();
        if(ok){
            shopService.clearShopCache(shopId);
            if(status==1){
                shopSearchService.indexById(shopId);//通过则进入索引
                shopService.saveShopGeo(shopId,shop.getTypeId(),shop.getX(), shop.getY());//商家进入GEO
            }else {
                shopSearchService.deleteById(shopId);//下架则出索引
                shopService.removeShopGeo(shopId,shop.getTypeId());//下架出GEO
            }
        }
        return ok?Result.ok():Result.fail("审核失败");
    }

    /**
     * 内部Feign:查询店铺所属商家
     */
    @GetMapping("/inner/owner/{id}")
    public Result queryOwner(@PathVariable("id") Long shopId){
        Shop shop=shopService.getById(shopId);
        if(shop==null){
            return Result.fail("店铺不存在");
        }else{
            return Result.ok(shop.getUserId());
        }
    }

    @GetMapping("/admin/geo/rebuild")
    @RequireRole("3")
    public Result rebuildGeo(){
        return shopService.rebuildGeo();
    }

}
