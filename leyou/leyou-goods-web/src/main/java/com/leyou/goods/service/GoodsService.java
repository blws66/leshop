package com.leyou.goods.service;

import com.leyou.goods.client.BrandClient;
import com.leyou.goods.client.CategoryClient;
import com.leyou.goods.client.GoodsClient;
import com.leyou.goods.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodsService {

        @Autowired
        private GoodsClient goodsClient;

        @Autowired
        private BrandClient brandClient;

        @Autowired
        private CategoryClient categoryClient;

        @Autowired
        private SpecificationClient specificationClient;

        public Map<String, Object> loadData(Long spuId){

        Map<String, Object> map = new HashMap<>();

        // 查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);
        // 查询spudetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);

        // 查询 分类
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names = this.categoryClient.queryNamesByIds(cids);
        // 创建一个分类的List<Map>
        List<Map<String, Object>> categoryMapList = new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", cids.get(i));
            categoryMap.put("name", names.get(i));
            categoryMapList.add(categoryMap);
        }

        // 查询品牌
        Brand brand = this.brandClient.queryBrandById(spu.getBrandId());

        // 查询skus
        List<Sku> skus = this.goodsClient.querySkusBySpuId(spuId);

        // 查询group
        List<SpecGroup> groups = this.specificationClient.queryGroupParamsByCid(spu.getCid3());

        // 查询特有的规格参数
        List<SpecParam> params = this.specificationClient.queryParamByGid(null, spu.getCid3(), false, null);
        Map<Long, String> paramMap = new HashMap<>();
        params.forEach(param -> {
            paramMap.put(param.getId(), param.getName());
        });

        // spu
        map.put("spu", spu);
        // spuDetail
        map.put("spuDetail", spuDetail);
        // 分类
        map.put("categories", categoryMapList);
        // 品牌
        map.put("brand", brand);
        // skus
        map.put("skus", skus);
        // 规格参数组，携带规格参数的
        map.put("groups", groups);
        // 特殊的规格参数{id：name}
        map.put("paramMap", paramMap);

        return map;
    }
}
