package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.mis.dto.role.vo.PermissionsVO;
import com.leehuang.his.api.mis.service.PermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/mis/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;

    @GetMapping("/searchAllPermission")
    @SaCheckPermission(value = {"ROOT", "ROLE:INSERT", "ROLE:UPDATE"}, mode = SaMode.OR)
    public R searchAllPermission(){
        List<PermissionsVO> allPermission = permissionService.getAllPermission();
        return R.OK().put("allPermission", allPermission);
    }
}
