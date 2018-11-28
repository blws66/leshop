package com.leyou.item.service;

import com.leyou.item.pojo.Category;
import com.leyou.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoryListByParentId(Long pid) {
        Category record = new Category();
        record.setParentId(pid);
        return this.categoryMapper.select(record);
    }

    public List<String> queryNamesByIsds(List<Long> ids) {
        return categoryMapper.selectByIdList(ids).stream().map(category -> category.getName()).collect(Collectors.toList());
    }
}
