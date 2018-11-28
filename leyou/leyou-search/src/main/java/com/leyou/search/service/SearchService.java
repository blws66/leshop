package com.leyou.search.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {
    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {
        Goods goods = new Goods();

        // 根据分类ids查询分类名称
        List<String> names = this.categoryClient.queryNamesByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        // 根据id查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询spu下的所有sku
        System.out.println(spu.getId());
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spu.getId());

        // 查询搜索字段
        List<SpecParam> params = this.specificationClient.queryParamByGid(null, spu.getCid3(), null, true);

        // 查询spudetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spu.getId());

        // 获取通用的规格参数，反序列化为Map<参数id, 参数值>
        Map<String, Object> genericSpecs = MAPPER.readValue(spuDetail.getGenericSpec(), new TypeReference<Map<String, Object>>() {
        });
        // 获取特殊的规格参数，反序列化为Map<参数id， sku可能的取值:List<Object>>
        Map<String, List<Object>> specialSpecs = MAPPER.readValue(spuDetail.getSpecialSpec(), new TypeReference<Map<String, List<Object>>>() {
        });

        // 遍历搜索的规格参数，获取对应的值。封装到Map<name,value>
        Map<String, Object> specMap = new HashMap<>();
        params.forEach(param -> {
            if (param.getGeneric()){
                String value = genericSpecs.get(param.getId().toString()).toString();
                if (param.getNumeric()){
                    value = chooseSegment(value, param);
                }
                specMap.put(param.getName(), value);
            } else {
                specMap.put(param.getName(), specialSpecs.get(param.getId().toString()));
            }
        });

        // 接收所有sku的价格
        List<Long> prices = new ArrayList<>();
        // 按需接收sku的字段
        List<Map<String, Object>> skuMapList = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            // 如果图片不存在，封装空字符串；存在，那就逗号分隔形成数组，获取第一张图片
            skuMap.put("image", sku.getImages() == null ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            skuMap.put("price", sku.getPrice());
            skuMapList.add(skuMap);
        });

        // 设置参数
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        // 搜索字段：标题 分类名称 品牌名称
        goods.setAll(spu.getTitle() + StringUtils.join(names, " ") + brand.getName());
        // 价格集合（sku）
        goods.setPrice(prices);
        // sku集合
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        // 搜索的规格参数（名称，值：集合，字符串，数字）
        goods.setSpecs(specMap);
        return goods;
    }
    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public PageResult<Goods> search(SearchRequest searchRequest) {
        //判断搜索条件和搜索关键字是否为空
        if(searchRequest==null||StringUtils.isBlank(searchRequest.getKey())){
            return null;
        }
        //构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加基本查询
        queryBuilder.withQuery(QueryBuilders.matchQuery("all",searchRequest.getKey()).operator(Operator.AND));
        //是指查询出的内容哪些显示
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));
        //设置分页
        Integer page = searchRequest.getPage();
        Integer size = searchRequest.getSize();
        queryBuilder.withPageable(PageRequest.of(page-1,size));
        //开启聚合
        queryBuilder.addAggregation(AggregationBuilders.terms("brands").field("brandId"));
        queryBuilder.addAggregation(AggregationBuilders.terms("categories").field("cid3"));
        //执行搜索，获取结果
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)goodsRepository.search(queryBuilder.build());
        //解析聚合结果集
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation("brands"));
        List<Map<String,Object>> categories = getCategoryAggResult(goodsPage.getAggregation("categories"));
        //封装
        return new SearchResult<Goods>(goodsPage.getTotalElements(), goodsPage.getContent(), Long.valueOf(goodsPage.getTotalPages()),categories,brands);
}

    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        ArrayList<Long> ids = new ArrayList<>();
        LongTerms terms = (LongTerms)aggregation;
        terms.getBuckets().forEach(bucket -> {
            ids.add(bucket.getKeyAsNumber().longValue());
        });
        List<Map<String,Object>> categories = new ArrayList<>();
        List<String> names = this.categoryClient.queryNamesByIds(ids);
        for (int i = 0;i<ids.size();i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id",ids.get(i));
            map.put("name",names.get(i));
            categories.add(map);
        }
        return categories;
    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        ArrayList<Long> ids = new ArrayList<>();
        LongTerms terms = (LongTerms)aggregation;
        terms.getBuckets().forEach(bucket -> {
            ids.add(bucket.getKeyAsNumber().longValue());
        });
        return this.brandClient.queryBrandsByIds(ids);
    }

    public void save(Long id) throws IOException {
        //构建spu
        Spu spu = goodsClient.querySpuById(id);
        //根据spu创建goods
        Goods goods = buildGoods(spu);
        //保存goods对象
        goodsRepository.save(goods);
    }
}
