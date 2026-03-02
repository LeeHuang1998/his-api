package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.CustomerEntity;
import com.leehuang.his.api.front.dto.customer.request.CustomerInfoRequest;
import com.leehuang.his.api.front.dto.customer.vo.CustomerVO;
import org.apache.ibatis.annotations.Param;


/**
* @author 16pro
* @description 针对表【tb_customer(客户表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.CustomerEntity
*/
public interface CustomerDao extends BaseMapper<CustomerEntity> {

    // 根据用户名查询用户
    CustomerEntity searchCustomerByUsernmae(@Param("identity") String identity);

    // 根据手机号查询用户
    CustomerEntity searchCustomerByTel(@Param("identity") String identity);

    // 注册
    int insertCustomer(CustomerEntity customer);

    // 获取用户个人信息
    CustomerVO getCustomerInfo(@Param("customerId") Integer customerId);

    // 更新用户信息
    void updateCustomerInfo(@Param("request") CustomerInfoRequest request, @Param("customerId") Integer customerId);

    // 更新手机号
    void updateTel(@Param("identity") String identity, @Param("customerId") int customerId);

    // 根据 id 获取用户登录信息
    CustomerEntity searchCustomerById(@Param("customerId") int customerId);

    // 根据 id 获取用户基本信息
    CustomerEntity searchCustomerBaseInfoById(@Param("customerId") int customerId);

    // 更新用户密码
    void updatePassword(@Param("newPassword") String newPassword, @Param("customerId") int customerId);

    // 更新照片
    void updatePhoto(@Param("customerId") int customerId, @Param("path") String path);


}




