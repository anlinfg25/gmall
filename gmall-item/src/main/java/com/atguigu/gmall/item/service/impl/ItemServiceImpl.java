package com.atguigu.gmall.item.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    /**
     * 异步编排求出商品详情
     * @param skuId
     * @return
     */
    @Override
    public ItemVO queryItemVO(Long skuId) {
        System.out.println("进入service");

        ItemVO itemVO = new ItemVO();

        //设置skuId
        itemVO.setSkuId(skuId);

        //赋值
        //根据skuId查询sku
        CompletableFuture<Object> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuResp = this.pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuResp.getData();
            if (skuInfoEntity == null) {
                return itemVO;
            }
            itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVO.setSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVO.setPrice(skuInfoEntity.getPrice());
            itemVO.setWeight(skuInfoEntity.getWeight());
            itemVO.setSpuId(skuInfoEntity.getSpuId());
            //获取spuId
            return skuInfoEntity;
        }, threadPoolExecutor);


        CompletableFuture<Void> spuCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            //根据sku中的spuId查询spu
            Resp<SpuInfoEntity> spuResp = this.pmsClient.querySpuById(((SkuInfoEntity) sku).getSpuId());
            SpuInfoEntity spuInfoEntity = spuResp.getData();

            if (spuInfoEntity != null) {
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);


        //根据skuId查询图片集
        CompletableFuture<Void> imageCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> skuImagesResp = this.pmsClient.querySkuImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = skuImagesResp.getData();
            itemVO.setPics(skuImagesEntities);
        }, threadPoolExecutor);


        //根据查出的spu的brandId和cateId查询品牌和分类
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(((SkuInfoEntity) sku).getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            itemVO.setBrandEntity(brandEntity);
        }, threadPoolExecutor);

        CompletableFuture<Void> cateCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById(((SkuInfoEntity) sku).getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            itemVO.setCategoryEntity(categoryEntity);
        }, threadPoolExecutor);


        //根据skuId查询促销信息
        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SaleVO>> salesResp = this.smsClient.querySkuSalesBySkuId(skuId);
            List<SaleVO> saleVOList = salesResp.getData();
            itemVO.setSales(saleVOList);
        }, threadPoolExecutor);


        //根据skuId查询库存信息
        CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> wareResp = this.wmsClient.queryWareSkusBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResp.getData();
            itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
        }, threadPoolExecutor);


        //根据spuId查询出旗下所有sku的所有销售属性
        CompletableFuture<Void> saleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            Resp<List<SkuSaleAttrValueEntity>> saleAttrValueResp = this.pmsClient.querySkuSaleAttrValuesBySpuId(((SkuInfoEntity) sku).getSpuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrValueResp.getData();
            itemVO.setSaleAttrs(skuSaleAttrValueEntities);
        }, threadPoolExecutor);


        //根据spuId查询商品描述(海报)
        CompletableFuture<Void> descCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = this.pmsClient.querySpuDescBySpuId(((SkuInfoEntity) sku).getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null) {
                String decript = spuInfoDescEntity.getDecript();
                String[] split = StringUtils.split(decript, ",");
                itemVO.setImages(Arrays.asList(split));
            }
        }, threadPoolExecutor);


        //根据spuId和分类Id查询组和组下的规格参数(带值)
        CompletableFuture<Void> groupCompletableFuture = skuCompletableFuture.thenAcceptAsync(sku -> {
            Resp<List<ItemGroupVO>> itemGroupVOResp = this.pmsClient.queryItemGroupVOByCidAndSpuId(((SkuInfoEntity) sku).getCatalogId(), ((SkuInfoEntity) sku).getSpuId());
            List<ItemGroupVO> itemGroupVOS = itemGroupVOResp.getData();
            itemVO.setGroups(itemGroupVOS);
        }, threadPoolExecutor);

        CompletableFuture.allOf(spuCompletableFuture, imageCompletableFuture, brandCompletableFuture,
                cateCompletableFuture, saleCompletableFuture, storeCompletableFuture,
                saleAttrCompletableFuture, descCompletableFuture,groupCompletableFuture).join();

        return itemVO;
    }
}
