package com.atguigu.gmall.sms.service;

import com.atguigu.gmall.sms.vo.SaleVO;
import com.atguigu.gmall.sms.vo.SkuSaleVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 商品sku积分设置
 *
 * @author anlin
 * @email anlin@atguigu.com
 * @date 2020-02-03 14:08:14
 */
public interface SkuBoundsService extends IService<SkuBoundsEntity> {

    PageVo queryPage(QueryCondition params);

    void saveSale(SkuSaleVO skuSaleVO);

    List<SaleVO> querySkuSalesBySkuId(Long skuId);
}

