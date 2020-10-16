package com.offcn.sellergoods.service;

import com.offcn.entity.PageResult;
import com.offcn.pojo.TbBrand;

import java.util.List;

public interface BrandService {

    public List<TbBrand> findAll();

    public PageResult findPages(int pageNum,int pageSize);

    public void save(TbBrand brand);

    public TbBrand findOne(long id);

    public void update(TbBrand brand);

    public void delete(long[] ids);

    public PageResult search(TbBrand brand,int pageNum,int pageSize);

    public List<TbBrand> selectOptionList();
}
