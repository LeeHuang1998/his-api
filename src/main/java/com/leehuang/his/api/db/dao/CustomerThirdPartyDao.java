package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.CustomerThirdPartyEntity;
import com.leehuang.his.api.front.dto.customer.CustomerBindDTO;
import org.apache.ibatis.annotations.Param;

/**
 * @author 16pro
 * @description 针对表【tb_customer_third_party】的数据库操作Mapper
 * @createDate 2025-07-15 15:45:32
 * @Entity com.leehuang.his.api.db.entity.ActionEntity
 */
public interface CustomerThirdPartyDao extends BaseMapper<CustomerThirdPartyEntity> {

    // 根据 openId 和 platform 查询第三方登录用户
    CustomerThirdPartyEntity findByOpenIdAndPlatform(@Param("openId") String openId, @Param("platform") String gitee);

    // 插入第三方登录用户
    void insertThirdPartyCustomer(CustomerThirdPartyEntity thirdPartyEntity);

    // 根据手机号和平台查询用户是否已绑定
    CustomerBindDTO searchByTelAndPlatform(@Param("phoneNum") String phoneNum, @Param("platform") String platform);
}
