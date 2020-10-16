package com.offcn.sellergoods.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.offcn.entity.PageResult;
import com.offcn.mapper.TbBrandMapper;
import com.offcn.pojo.TbBrand;
import com.offcn.pojo.TbBrandExample;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.offcn.sellergoods.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class BrandServiceImpl implements BrandService {

    @Autowired
    private TbBrandMapper brandMapper;

    @Override
    public List<TbBrand> findAll() {
        return brandMapper.selectByExample(null);
    }

    @Override
    public PageResult findPages(int pageNum, int pageSize) {

        //1、设置分页
        PageHelper.startPage(pageNum,pageSize);
        //2、查询
        Page<TbBrand> page = (Page)brandMapper.selectByExample(null);

        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public void save(TbBrand brand) {
        brandMapper.insert(brand);
    }

    @Override
    public TbBrand findOne(long id) {
        return brandMapper.selectByPrimaryKey(id);
    }

    @Override
    public void update(TbBrand brand) {
        brandMapper.updateByPrimaryKey(brand);
    }

    @Override
    public void delete(long[] ids) {
        for (long id : ids) {
            brandMapper.deleteByPrimaryKey(id);
        }
    }

    @Override
    public PageResult search(TbBrand brand, int pageNum, int pageSize) {
        //1、pageHelper
        PageHelper.startPage(pageNum,pageSize);
        //2、拼接条件
        TbBrandExample example = new TbBrandExample();
        TbBrandExample.Criteria createria =  example.createCriteria();

        if(brand != null){
            if(brand.getName()!=null && brand.getName().length()>0){
                createria.andNameLike("%"+brand.getName()+"%");
            }
            if(brand.getFirstChar()!=null && brand.getFirstChar().length()>0){
                createria.andFirstCharEqualTo(brand.getFirstChar());
            }
        }

        Page<TbBrand> page = (Page<TbBrand>) brandMapper.selectByExample(example);

        return new PageResult(page.getTotal(),page.getResult());
    }

    @Override
    public List<TbBrand> selectOptionList() {
        return brandMapper.selectOptionList();
    }
}
