package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Document(indexName = "goods", type = "info", shards = 3, replicas = 2)
@Data
public class Goods {

    @Id
    private Long skuId; //skuid

    @Field(type = FieldType.Keyword,index = false)
    private String pic;  //主图

    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String title;  //标题

    @Field(type = FieldType.Double)
    private double price;  //价钱

    @Field(type = FieldType.Long)
    private Long sale;  //销量

    @Field(type = FieldType.Boolean)
    private Boolean store; //是否有货

    @Field(type = FieldType.Date)
    private Date createTime;  //新品----创建时间

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Nested)
    private List<SearchAttr> attrs;




}
