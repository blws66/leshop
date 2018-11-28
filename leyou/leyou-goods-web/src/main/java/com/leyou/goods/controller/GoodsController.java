package com.leyou.goods.controller;

import com.leyou.goods.service.GoodsHtmlService;
import com.leyou.goods.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("item")
public class GoodsController {
    @Autowired
    private GoodsService goodsService;
    @Autowired
    private GoodsHtmlService goodsHtmlService;
    @GetMapping("{id}.html")
    public String toItem(@PathVariable("id")Long id, Model model){
        //查询数据
        Map<String, Object> map = this.goodsService.loadData(id);
        //设置数据模型
        model.addAllAttributes(map);
        //生成静态化页面(如何保证静态化之后不再进入当前controller，nginx进行拦截若有静态化页面则不进入当前controller)
        goodsHtmlService.createHtml(id);
        //跳转页面
        return "item";
    }
}
