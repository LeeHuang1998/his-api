package com.leehuang.his.api.mis.service.impl;

import com.leehuang.his.api.db.dao.PermissionDao;
import com.leehuang.his.api.mis.dto.role.vo.PermissionsVO;
import com.leehuang.his.api.mis.service.PermissionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service("permissionService")
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private PermissionDao permissionDao;

    @Override
    public List<PermissionsVO> getAllPermission() {
        return permissionDao.getAllPermission();
    }
}
