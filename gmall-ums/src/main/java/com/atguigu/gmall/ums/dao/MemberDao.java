package com.atguigu.gmall.ums.dao;

import com.atguigu.gmall.ums.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author anlin
 * @email anlin@atguigu.com
 * @date 2020-02-23 12:40:56
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
