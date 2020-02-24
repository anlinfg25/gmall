package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVO;
import com.atguigu.gmall.search.service.SearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SearchResponseVO search(SearchParam searchParam) throws IOException {

        //构建dsl语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchResponseVO responseVO = this.parseSearchResult(response);
        responseVO.setPageSize(searchParam.getPageSize());
        responseVO.setPageNum(searchParam.getPageNum());

        return responseVO;
    }


    //处理搜索后的结果，信息赋值给实体类
    private SearchResponseVO parseSearchResult(SearchResponse response) throws JsonProcessingException {

        SearchResponseVO responseVO = new SearchResponseVO();

        //获取总记录数
        SearchHits hits = response.getHits();
        responseVO.setTotal(hits.totalHits);

        //解析品牌的聚合结果集
        SearchResponseAttrVO brand = new SearchResponseAttrVO();
        brand.setName("品牌");
        //获取全部的聚合结果集
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        //获取品牌-brandIdAgg的结果集
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggregationMap.get("brandIdAgg");
        //获取brandIdAgg下的桶
        List<String> brandValues = brandIdAgg.getBuckets().stream().map(bucket -> {

            Map<String,String> map = new HashMap<>();
            //获取品牌id
            map.put("id",bucket.getKeyAsString());
            //获取品牌名称--通过子聚合来获取,只有一个name
            Map<String, Aggregation> brandIdSubMap = bucket.getAggregations().asMap();
            ParsedStringTerms brandNameAgg = (ParsedStringTerms)brandIdSubMap.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name",brandName);


            try {
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                //问题
                return null;
            }
        }).collect(Collectors.toList());


        brand.setValue(brandValues);
        responseVO.setBrand(brand);


        //解析分类的聚合结果集
        SearchResponseAttrVO category = new SearchResponseAttrVO();
        brand.setName("分类");

        //获取分类-categoryIdAgg的结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggregationMap.get("categoryIdAgg");
        //获取categoryIdAgg下的桶
        List<String> categoryValues = categoryIdAgg.getBuckets().stream().map(bucket -> {

            Map<String,String> map = new HashMap<>();
            //获取分类id
            map.put("id",bucket.getKeyAsString());
            //获取分类名称--通过子聚合来获取,只有一个name
            Map<String, Aggregation> categoryIdSubMap = bucket.getAggregations().asMap();
            ParsedStringTerms categoryNameAgg = (ParsedStringTerms)categoryIdSubMap.get("categoryNameAgg");
            String categoryName = categoryNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name",categoryName);

            try {
                return OBJECT_MAPPER.writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                //问题
                return null;
            }
        }).collect(Collectors.toList());
        //赋值
        category.setValue(categoryValues);
        responseVO.setCatelog(category);


        //解析查询列表(商品)
        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit subHit : subHits) {
            //Goods goods = gson.fromJson(subHit.getSourceAsString(), Goods.class);
            Goods goods = OBJECT_MAPPER.readValue(subHit.getSourceAsString(), new TypeReference<Goods>() {
            });
            goods.setTitle(subHit.getHighlightFields().get("title").getFragments()[0].toString());
            goodsList.add(goods);
        }
        responseVO.setProducts(goodsList);

        //解析规格参数
        //获取嵌套聚合对象
        ParsedNested attrAgg = (ParsedNested)aggregationMap.get("attrAgg");
        //获取规格id的聚合对象
        ParsedLongTerms attrIdAgg = (ParsedLongTerms)attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseAttrVO> responseAttrVOList = buckets.stream().map(bucket -> {
                SearchResponseAttrVO responseAttrVO = new SearchResponseAttrVO();
                //设置规格参数的id
                responseAttrVO.setProductAttributeId(bucket.getKeyAsNumber().longValue());
                //设置规格参数的名称
                List<? extends Terms.Bucket> attrNameBuckets = ((ParsedStringTerms) (bucket.getAggregations().get("attrNameAgg"))).getBuckets();
                responseAttrVO.setName(attrNameBuckets.get(0).getKeyAsString());
                //设置规格参数值得列表
                List<? extends Terms.Bucket> attrValueBuckets = ((ParsedStringTerms) (bucket.getAggregations().get("attrValueAgg"))).getBuckets();
                List<String> attrValues = attrValueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                responseAttrVO.setValue(attrValues);
                return responseAttrVO;
            }).collect(Collectors.toList());
            responseVO.setAttrs(responseAttrVOList);
        }

        return responseVO;
    }

    //搜索语句
    private SearchRequest buildQueryDsl(SearchParam searchParam){
        //查询关键字
        String keyword = searchParam.getKeyword();
        if(StringUtils.isEmpty(keyword)){
            return null;
        }

        //查询条件构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //1.构建查询条件和过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1构建查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //1.2构建过滤条件
        //1.2.1构建品牌过滤
        String[] brand = searchParam.getBrand();
        if(brand != null && brand.length!=0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brand));
        }
        //1.2.2构建分类过滤
        String[] catelog3 = searchParam.getCatelog3();
        if(catelog3 != null && catelog3.length!=0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",catelog3));
        }
        //1.2.3构建规格属性的嵌套过滤
        String[] props = searchParam.getProps();
        if(props != null && props.length!=0){
            for (String prop : props) {
                //已:分割字符串,分割后应该是2个元素，1-attrId 2-attrValue(已-分隔的字符串)
                String[] split = StringUtils.split(prop, ":");
                if(split == null || split.length!= 2){
                    continue;
                }
                //已-分割字符串处理attrValue
                String[] attrValues = StringUtils.split(split[1], "-");

                //构建嵌套查询
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //构建嵌套查询中的子查询
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                //构建嵌套查询中的子查询的过滤条件 规格id和规格值数组
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                subBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                //把嵌套查询放入过滤器中
                boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                boolQueryBuilder.filter(boolQuery);
            }
        }
        //1.2.4价格区间过滤
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price");
        Integer priceFrom = searchParam.getPriceFrom();
        Integer priceTo = searchParam.getPriceTo();
        if(priceFrom != null){
            rangeQueryBuilder.gte(priceFrom);
        }
        if(priceTo != null){
            rangeQueryBuilder.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQueryBuilder);

        //放入查询条件和过滤条件
        sourceBuilder.query(boolQueryBuilder);

        //2.构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum-1) * pageSize);
        sourceBuilder.size(pageSize);

        //3.构建排序
        String order = searchParam.getOrder();
        if(!StringUtils.isNoneBlank(order)){
            //已:进行分隔
            String[] split = StringUtils.split(order, ":");
            if(split!=null && split.length == 2){
                String field = null;
                switch (split[0]){
                    case "1":
                        field = "sale";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                sourceBuilder.sort(field,StringUtils.equals("asc",split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }

        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em>").postTags("</em>"));

        //5.构建聚合
        //5.1品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));
        //5.2分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.2搜索的规格属性聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        System.out.println("输出啦:"+sourceBuilder.toString());

        //结果集过滤,只剩下这四个
        sourceBuilder.fetchSource(new String[]{"skuId","pic","title","price"},null);

        //查询参数
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);

        return searchRequest;
    }
}
