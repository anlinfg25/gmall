package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.SaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVO {

    private Long skuId;

    private CategoryEntity categoryEntity;  //分类

    private BrandEntity brandEntity;  //品牌

    private Long spuId;

    private String spuName;  //大类名称

    private String skuTitle;  //标题
    private String subTitle;  //副标题
    private BigDecimal price;  //价格
    private BigDecimal weight;  //重量

    private List<SkuImagesEntity> pics; //sku图片集

    private List<SaleVO> sales; //营销信息

    private Boolean store; //是否有货

    private List<SkuSaleAttrValueEntity> saleAttrs; //销售属性

    //下半部,商品详情介绍
    private List<String> images; //spu的海报


    private List<ItemGroupVO> groups;  //规格参数组及组下的规格参数(带值)








}
