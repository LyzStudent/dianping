package com.dianping.shop.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "shop")
public class ShopDocument {

    @Id
    private Long id;
    private String name;
    private Long typeId;
    private String area;
    private String address;
    private Long avgPrice;
    private Integer sold;
    private Integer comments;
    private Integer score;
    private Double x;
    private Double y;
    private String openTime;
    private String images;

}
