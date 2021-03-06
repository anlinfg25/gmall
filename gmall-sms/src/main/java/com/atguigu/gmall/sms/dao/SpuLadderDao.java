package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.SpuLadderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品阶梯价格
 * 
 * @author anlin
 * @email anlin@atguigu.com
 * @date 2020-02-03 14:08:13
 */
@Mapper
public interface SpuLadderDao extends BaseMapper<SpuLadderEntity> {
	
}
