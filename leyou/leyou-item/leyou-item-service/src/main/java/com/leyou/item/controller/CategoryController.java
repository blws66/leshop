package com.leyou.item.controller;

import com.leyou.item.pojo.Category;
import com.leyou.item.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("list")
    public ResponseEntity<List<Category>> queryCategoryListByParentId(@RequestParam(value="pid",defaultValue = "0")Long pid){
        try {
            if(pid==null||pid.longValue()<0){
                return ResponseEntity.badRequest().build();
            }
            List<Category> categoryList = this.categoryService.queryCategoryListByParentId(pid);
            if(CollectionUtils.isEmpty(categoryList)){
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(categoryList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    @GetMapping("query")
    public ResponseEntity<List<String>> queryNamesByIds(@RequestParam("ids")List<Long> ids){
        List<String> categoryNames = categoryService.queryNamesByIsds(ids);
        if(CollectionUtils.isEmpty(categoryNames)){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(categoryNames);
    }
}
