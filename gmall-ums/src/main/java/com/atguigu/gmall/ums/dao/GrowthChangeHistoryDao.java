package com.atguigu.gmall.ums.dao;

import com.atguigu.gmall.ums.entity.GrowthChangeHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 成长值变化历史记录
 * 
 * @author anlin
 * @email anlin@atguigu.com
 * @date 2020-02-23 12:40:57
 */
@Mapper
public interface GrowthChangeHistoryDao extends BaseMapper<GrowthChangeHistoryEntity> {
	
}
