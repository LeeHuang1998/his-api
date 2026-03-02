package com.leehuang.his.api.front.service.impl;

import com.leehuang.his.api.db.dao.CustomerThirdPartyDao;
import com.leehuang.his.api.db.entity.CustomerThirdPartyEntity;
import com.leehuang.his.api.front.dto.customer.CustomerBindDTO;
import com.leehuang.his.api.front.service.CustomerThirdPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("customerThirdPartyService")
@RequiredArgsConstructor
public class CustomerThirdPartyServiceImpl implements CustomerThirdPartyService {

    private final CustomerThirdPartyDao thirdPartyDao;

    /**
     * 根据 openId 和 platform 查找是否有该第三方用户
     * @param openId
     * @param platform
     * @return
     */
    @Override
    public CustomerThirdPartyEntity findByOpenIdAndPlatform(String openId, String platform) {
        return thirdPartyDao.findByOpenIdAndPlatform(openId, platform);
    }

    /**
     * 插入第三方用户
     * @param thirdPartyEntity
     */
    @Override
    @Transactional
    public void insertThirdPartyCustomer(CustomerThirdPartyEntity thirdPartyEntity) {
        thirdPartyDao.insertThirdPartyCustomer(thirdPartyEntity);
    }

    @Override
    public CustomerBindDTO searchByTelAndPlatform(String phoneNum, String platform) {
        return thirdPartyDao.searchByTelAndPlatform(phoneNum, platform);
    }
}
