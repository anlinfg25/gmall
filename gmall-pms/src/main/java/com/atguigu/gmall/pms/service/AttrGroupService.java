package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVO;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author anlin
 * @email 312521538@qq.com
 * @date 2020-02-02 13:29:44
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByPage(QueryCondition condition, Long catId);

    GroupVO queryGroupWithAttrByGid(Long gid);

    List<GroupVO> queryGroupWithAttrsByCid(Long cid);

    List<ItemGroupVO> queryItemGroupVOByCidAndSpuId(Long cid, Long spuId);
}

