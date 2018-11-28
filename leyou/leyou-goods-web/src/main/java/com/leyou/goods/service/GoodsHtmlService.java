package com.leyou.goods.service;

import com.leyou.goods.utils.ThreadUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

@Service
public class GoodsHtmlService {
    @Autowired
    private TemplateEngine engine;
    @Autowired
    private GoodsService goodsService;
    public void createHtml(Long spuId){
        PrintWriter writer = null;
        try {
            Map<String, Object> map = goodsService.loadData(spuId);
            Context context = new Context();
            context.setVariables(map);
            File file = new File("E:\\nginx-1.14.0\\html\\item\\" + spuId + ".html");
            writer = new PrintWriter(file);
            engine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally{
            if(writer != null){
                writer.close();
            }
        }
    }
    public void asyncExcute(Long spuId){
        ThreadUtils.execute(()->createHtml(spuId));
        /*ThreadUtils.execute(new Runnable() {
            @Override
            public void run() {
                createHtml(spuId);
            }
        });*/
    }
}
