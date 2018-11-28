package com.leyou.item.service;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.mapper.SpecGroupMapper;
import com.leyou.item.mapper.SpecParamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class SpecificationService {
    @Autowired
    private SpecGroupMapper specGroupMapper;
    @Autowired
    private SpecParamMapper specParamMapper;

    public List<SpecGroup> queryGroupsByCid(Long cid) {
        SpecGroup group = new SpecGroup();
        group.setCid(cid);
        List<SpecGroup> groups = specGroupMapper.select(group);
        return groups;
    }

    public List<SpecParam> queryParamByGid(Long gid, Long cid, Boolean generic, Boolean searching) {
        SpecParam param = new SpecParam();
        param.setGroupId(gid);
        param.setCid(cid);
        param.setGeneric(generic);
        param.setSearching(searching);
        List<SpecParam> params = specParamMapper.select(param);
        return params;
    }

    public List<SpecGroup> queryGroupParamsByCid(Long cid) {
        SpecGroup record = new SpecGroup();
        record.setCid(cid);
        List<SpecGroup> groups = this.specGroupMapper.select(record);
        groups.forEach(group->{
            SpecParam param = new SpecParam();
            param.setGroupId(group.getId());
            List<SpecParam> params = specParamMapper.select(param);
            group.setParams(params);
        });
        return groups;
    }
}
