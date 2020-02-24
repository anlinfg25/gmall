package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.vo.GroupVO;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrDao attrDao;

    @Autowired
    private ProductAttrValueDao attrValueDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByPage(QueryCondition condition, Long catId) {
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<>();
        if (catId != null) {
            wrapper.eq("catelog_id",catId);
        }
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(condition),
                wrapper
        );

        return new PageVo(page);
    }

    @Override
    public GroupVO queryGroupWithAttrByGid(Long gid) {

        GroupVO groupVO = new GroupVO();
        //查询group
        AttrGroupEntity groupEntity = this.getById(gid);
        BeanUtils.copyProperties(groupEntity,groupVO);

        //根据gid查询关联关系，并获取到attrIds
        List<AttrAttrgroupRelationEntity> relations = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", gid));
        if (CollectionUtils.isEmpty(relations)) {
            return groupVO;
        }
        groupVO.setRelations(relations);

        //根据attrIds查询所有的规格参数
        List<Long> attrIds = relations.stream().map(relationEntity -> relationEntity.getAttrId()).collect(Collectors.toList());
        List<AttrEntity> attrEntities = this.attrDao.selectBatchIds(attrIds);
        groupVO.setAttrEntities(attrEntities);

        return groupVO;
    }

    @Override
    public List<GroupVO> queryGroupWithAttrsByCid(Long cid) {

        //根据cid查询三级分类下的所有属性分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));

        //1、根据分组中的id查询中间表
        //2、根据中间表中的attrIds查询参数
        //3、数据类型的转化:AttrGroupEntity----GroupVO
        return groupEntities.stream().map(attrGroupEntity ->
            this.queryGroupWithAttrByGid(attrGroupEntity.getAttrGroupId())
        ).collect(Collectors.toList());
    }

    @Override
    public List<ItemGroupVO> queryItemGroupVOByCidAndSpuId(Long cid, Long spuId) {

        //通过分类id查询出全部组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));
        return attrGroupEntities.stream().map(group -> {

            ItemGroupVO itemGroupVO = new ItemGroupVO();

            itemGroupVO.setName(group.getAttrGroupName());

            //查询规格参数及值
            //通过组id查询中间表求出全部的attrIds
            List<AttrAttrgroupRelationEntity> relationEntities = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", group.getAttrGroupId()));
            List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());

            //通过attrIds和spuId查询出对应的属性名和属性值
            List<ProductAttrValueEntity> productAttrValueEntities = this.attrValueDao.selectList(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
            itemGroupVO.setBaseAttrs(productAttrValueEntities);
            return itemGroupVO;
        }).collect(Collectors.toList());
    }

}