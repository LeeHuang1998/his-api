package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.mis.dto.role.vo.PermissionsVO;

import java.util.List;

public interface PermissionService {

    // 获取所有权限
    List<PermissionsVO> getAllPermission();
}
