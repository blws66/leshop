package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {
    @Autowired
    private SpuMapper spuMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SpuDetailMapper spuDetailMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private AmqpTemplate amqpTemplate;

    public PageResult<SpuBo> querySpuBoByPage(String key, Boolean saleable, Integer page, Integer rows) {
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //判断模糊查询
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        //判断是否上下架
        if (saleable != null) {
            criteria.andEqualTo("saleable", saleable);
        }
        //开启分页
        PageHelper.startPage(page,rows);
        //查询spu
        List<Spu> spuList = spuMapper.selectByExample(example);
        //新建spubo集合
        ArrayList<SpuBo> spuBoList = new ArrayList<SpuBo>();
        //总页数
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spuList);
        long pageTotal = spuPageInfo.getTotal();

        spuList.forEach(spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu,spuBo);
            //查询bname
            Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());
            //查询cname
            List<Category> categories = categoryMapper.selectByIdList(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
            //将所有category转换成对应的category.getName，并转换成List
            List<String> cnameParamList = categories.stream().map(category -> category.getName()).collect(Collectors.toList());
            spuBo.setCname(StringUtils.join(cnameParamList,"-"));
            spuBoList.add(spuBo);
        });
        return new PageResult<>(pageTotal,spuBoList);
    }
    @Transactional
    public void saveGoods(SpuBo spuBo) {
        //保存spu
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        spuMapper.insertSelective(spuBo);
        //保存spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        spuDetailMapper.insertSelective(spuDetail);
        saveSkuAndStock(spuBo);
        sendMessage("insert",spuBo.getId());
    }

    private void sendMessage(String type,Long id) {
        try {
            this.amqpTemplate.convertAndSend("item."+type,id);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        //保存sku
        List<Sku> skus = spuBo.getSkus();
        skus.forEach(sku -> {
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insertSelective(sku);
            //保存stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insertSelective(stock);
        });
    }

    public SpuDetail querySpuDetailBySpuId(Long spuId) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(spuId);
        return spuDetail;
    }

    public List<Sku> querySkusBySpuId(Long id) {
        Sku record = new Sku();
        record.setSpuId(id);
        List<Sku> skus = skuMapper.select(record);
        skus.forEach(sku -> {
            sku.setStock(stockMapper.selectByPrimaryKey(sku.getId()).getStock());
        });
        return skus;
    }

    @Transactional
    public void updateGoods(SpuBo spuBo) {
        // 根据spuId先查询sku
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        List<Sku> skus = this.skuMapper.select(sku);
        List<Long> skuIds = skus.stream().map(sku1 -> sku1.getId()).collect(Collectors.toList());

        // 先删除stock
        Example stockExample = new Example(Stock.class);
        stockExample.createCriteria().andIn("skuId", skuIds);
        this.stockMapper.deleteByExample(stockExample);

        // 再删除sku
        /*Example skuExample = new Example(Sku.class);
        skuExample.createCriteria().andIn("id", skuIds);*/
        Sku record = new Sku();
        record.setSpuId(spuBo.getId());
        this.skuMapper.delete(record);

        // 更新spu
        spuBo.setLastUpdateTime(new Date());
        this.spuMapper.updateByPrimaryKeySelective(spuBo);

        // 更新spuDetail
        this.spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
        saveSkuAndStock(spuBo);
        sendMessage("update",spuBo.getId());
    }

    public Spu querySpuById(Long id) {
        Spu spu = this.spuMapper.selectByPrimaryKey(id);
        return spu;
    }

    public Sku querySkuById(Long id) {
        Sku sku = this.skuMapper.selectByPrimaryKey(id);
        return sku;
    }
}
