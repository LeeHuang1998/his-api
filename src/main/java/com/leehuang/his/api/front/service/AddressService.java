package com.leehuang.his.api.front.service;

import com.leehuang.his.api.front.dto.address.request.AddressRequest;
import com.leehuang.his.api.front.dto.address.request.AddressStatusRequest;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface AddressService {
    // 添加或修改地址
    void saveAddress(AddressRequest request);

    // 删除地址
    int deleteAddress(Integer[] ids);

    // 设置默认地址
    void updateAddressDefault(@Valid AddressStatusRequest request);

    // 获取用户地址列表
    List<AddressVO> getAddressListByCustomerId();
}
