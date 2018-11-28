package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.Brand;
import com.leyou.item.mapper.BrandMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {
    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandsByPage(String key, Integer page, Integer rows, String sortBy, boolean desc) {
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
            //模糊查询
            example.createCriteria().andLike("name","%"+key+"%").orEqualTo("letter",key);
        }
        //List<Brand> brandList = this.brandMapper.selectByExample(example);    // 错误
        //分页查询
        PageHelper.startPage(page,rows);
        List<Brand> brandList = this.brandMapper.selectByExample(example);
        //添加排序
        if(StringUtils.isNotBlank(sortBy)){
            example.setOrderByClause(sortBy+(desc?" desc":" asc"));
        }
        PageInfo<Brand> pageInfo = new PageInfo<>(brandList);
        return new PageResult<Brand>(pageInfo.getTotal(),pageInfo.getList());
    }
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        brandMapper.insertSelective(brand);
        Long bid = brand.getId();
        cids.forEach(cid->{
            brandMapper.saveCategoryBrand(cid,bid);
        });
    }

    public List<Brand> queryBrandsByCid3(Long cid) {
        List<Brand> brands = this.brandMapper.queryBrandsByCid3(cid);
        return brands;
    }

    public Brand queryBrandById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        return brand;
    }

    public List<Brand> queryNamesByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        return brands;
    }
}
