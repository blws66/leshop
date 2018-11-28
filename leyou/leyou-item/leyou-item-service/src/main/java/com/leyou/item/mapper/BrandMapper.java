package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand>,SelectByIdListMapper<Brand,Long>{
    @Insert("insert into tb_category_brand values(#{cid},#{bid})")
    public void saveCategoryBrand(@Param("cid") Long cid,@Param("bid") Long bid);
    @Select("SELECT b.* FROM tb_brand b INNER JOIN tb_category_brand c ON b.id=c.brand_id WHERE c.category_id=#{cid}")
    public List<Brand> queryBrandsByCid3(@Param("cid")Long cid);
}
