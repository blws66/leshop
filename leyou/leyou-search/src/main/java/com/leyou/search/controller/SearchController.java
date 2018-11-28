package com.leyou.search.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class SearchController {
    @Autowired
    private SearchService searchService;
    @PostMapping
    public ResponseEntity<PageResult<Goods>> search(@RequestBody SearchRequest searchRequest){
        PageResult<Goods> pageResult = searchService.search(searchRequest);
//        if(CollectionUtils.isEmpty(pageResult.getItems())){
//            return ResponseEntity.notFound().build();
//        }
        return ResponseEntity.ok(pageResult);
    }
}
