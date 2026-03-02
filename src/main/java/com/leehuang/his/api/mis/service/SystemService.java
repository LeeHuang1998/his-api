package com.leehuang.his.api.mis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.db.entity.SystemEntity;

public interface SystemService extends IService<SystemEntity> {

    // 获取系统设置项的值
    String getSystemSettingItemValue(String item);

    Integer setSystemSettingItemValue(String item, String value);
}
