package com.leehuang.his.api.front.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.db.dao.AddressDao;
import com.leehuang.his.api.db.entity.AddressEntity;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.front.dto.address.request.AddressRequest;
import com.leehuang.his.api.front.dto.address.request.AddressStatusRequest;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import com.leehuang.his.api.front.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("AddressService")
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressDao addressDao;


    /**
     * 添加或修改地址
     * @param request
     */
    @Override
    @Transactional
    public void saveAddress(AddressRequest request) {
        Integer id = request.getId();
        int customerId = StpCustomerUtil.getLoginIdAsInt();

        AddressEntity addressEntity = new AddressEntity();
        BeanUtil.copyProperties(request, addressEntity, "regionCode");

        // 转换 regionCode 数组为 JSON 字符串
        String regionCode = JSON.toJSONString(request.getRegionCode());
        addressEntity.setRegionCode(regionCode);
        addressEntity.setCustomerId(customerId);
        addressEntity.setIsDefault(false);

        if (addressEntity.getId() == null) {
            // 添加地址
            addressDao.insertAddress(addressEntity);
        } else {
            // 修改地址

            addressDao.updateAddress(addressEntity);
        }
    }

    /**
     * 删除地址
     * @param ids
     */
    @Override
    @Transactional
    public int deleteAddress(Integer[] ids) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        int rows = addressDao.deleteAddress(ids, customerId);

        if (rows != ids.length){
            throw new HisException("提交的数据和删除的数据不一致");
        }

        return rows;
    }

    /**
     * 设置默认地址
     * @param request
     */
    @Override
    public void updateAddressDefault(AddressStatusRequest request) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        addressDao.updateAddressDefault(request, customerId);
    }

    /**
     * 获取用户地址列表
     * @return
     */
    @Override
    public List<AddressVO> getAddressListByCustomerId() {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        return addressDao.getAddressListByCustomerId(customerId);
    }
}
