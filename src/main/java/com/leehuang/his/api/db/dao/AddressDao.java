package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.AddressEntity;
import com.leehuang.his.api.front.dto.address.request.AddressStatusRequest;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 16pro
 * @description 针对表【tb_address(行为表)】的数据库操作Mapper
 * @createDate 2025-07-15 15:45:32
 * @Entity com.leehuang.his.api.db.entity.AddressEntity
 */
public interface AddressDao extends BaseMapper<AddressEntity> {
    // 添加地址
    void insertAddress(AddressEntity entity);

    // 更新地址
    void updateAddress(AddressEntity entity);

    // 删除地址
    int deleteAddress(@Param("ids") Integer[] ids, @Param("customerId") int customerId);

    // 设置默认地址
    void updateAddressDefault(@Param("request")AddressStatusRequest request, @Param("customerId") int customerId);

    // 获取默认地址
    AddressVO getDefaultAddress(@Param("customerId") Integer customerId);

    // 获取指定地址
    AddressVO getAddressById(@Param("id") Integer id, @Param("customerId") Integer customerId);

    // 获取地址列表
    List<AddressVO> getAddressListByCustomerId(@Param("customerId") Integer customerId);
}
