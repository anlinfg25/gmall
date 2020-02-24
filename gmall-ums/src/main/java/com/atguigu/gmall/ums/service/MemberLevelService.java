package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberLevelEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 会员等级
 *
 * @author anlin
 * @email anlin@atguigu.com
 * @date 2020-02-23 12:40:56
 */
public interface MemberLevelService extends IService<MemberLevelEntity> {

    PageVo queryPage(QueryCondition params);
}

