package com.leyou.search.test;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.SpuBo;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.repository.GoodsRepository;
import com.leyou.search.service.SearchService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SearchTest {
    //注入模板
    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SearchService searchService;
    @Autowired
    private GoodsRepository goodsRepository;
    //创建索引库
    @Test
    public void createIndex(){
        template.createIndex(Goods.class);
        template.putMapping(Goods.class);
        Integer page = 1;
        Integer rows = 100;
        do{
            //获得spu
            PageResult<SpuBo> spuBoPageResult = goodsClient.querySpuBoByPage(null, true, page, rows);
            List<SpuBo> items = spuBoPageResult.getItems();
            //初始化goodsList
            List<Goods> goodsList = items.stream().map(spuBo -> {
                try {
                    return searchService.buildGoods(spuBo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            //保存数据
            goodsRepository.saveAll(goodsList);
            rows=items.size();
            page++;
        }while(rows == 100);
    }
}
