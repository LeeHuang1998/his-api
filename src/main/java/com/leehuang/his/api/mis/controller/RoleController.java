package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.db.entity.RoleEntity;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.mis.dto.role.request.RolePageRequest;
import com.leehuang.his.api.mis.dto.role.request.RoleRequest;
import com.leehuang.his.api.mis.dto.role.vo.RolePageVO;
import com.leehuang.his.api.mis.dto.role.vo.RoleDetailVO;
import com.leehuang.his.api.mis.service.RoleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/mis/role")
public class RoleController {
    @Resource
    private RoleService roleService;

    // 查询所有角色
    @GetMapping("/getAllRoles")
    public R getAllRoles() {
        List<RoleEntity> allRoles = roleService.getAllRoles();
        return R.OK().put("roleList", allRoles);
    }

    /**
     * 分页查询角色数据
     * @param request   分页查询参数
     * @return          角色分页数据
     */
    @PostMapping("/getRoleListByPage")
    @SaCheckPermission(value = {"ROOT", "ROLE:SELECT"}, mode = SaMode.OR)
    public R getRoleListByPage(@RequestBody @Valid RolePageRequest request) {
        PageUtils<RolePageVO> roleListByPage = roleService.getRoleListByPage(request);
        return R.OK().put("rolePageData", roleListByPage);
    }

    /**
     * 根据 id 查询角色
     * @param request    角色 id
     * @return           角色信息
     */
    @PostMapping("/getRoleById")
    @SaCheckPermission(value = {"ROOT", "ROLE:UPDATE"}, mode = SaMode.OR)
    public R getRoleById(@RequestBody @Valid IdRequest request) {
        RoleDetailVO role = roleService.getRoleById(request.getId());
        return R.OK().put("role", role);
    }

    /**
     * 新增角色
     * @param request    新增角色请求参数
     * @return           新增角色影响的行数
     */
    @PostMapping("/insertRole")
    @SaCheckPermission(value = {"ROOT", "ROLE:INSERT"}, mode = SaMode.OR)
    public R insertRole(@RequestBody @Valid RoleRequest request){
        int rows = roleService.insertRole(request);
        return R.OK().put("rows", rows).put("msg", "新增角色成功");
    }

    /**
     * 修改角色信息
     * @param request   角色 id
     * @return          修改角色影响的行数
     */
    @PostMapping("/updateRole")
    @SaCheckPermission(value = {"ROOT", "ROLE:UPDATE"}, mode = SaMode.OR)
    public R updateRole(@RequestBody @Valid RoleRequest request){
        if (request.getChanged()){
            int rows = roleService.updateRole(request);
            if (rows == 1){
                List<Integer> userIdList = roleService.getUserIdByRoleId(request.getId());
                for (Integer id : userIdList) {
                    StpUtil.logout(id);
                }
                return R.OK().put("rows", rows).put("msg", "角色信息修改成功");
            } else {
                return R.error().put("msg", "出现错误，角色信息没有修改");
            }
        } else {
            return R.OK().put("rows", 0).put("msg", "角色信息没有修改");
        }
    }

    /**
     * 删除角色
     * @param request   角色 id 数组
     * @return          删除角色影响的行数
     */
    @PostMapping("/deleteRole")
    @SaCheckPermission(value = {"ROOT", "ROLE:DELETE"}, mode = SaMode.OR)
    public R deleteRole(@RequestBody @Valid IdsRequest request) {
        int rows = roleService.deleteRole(request.getIds());
        return R.OK().put("rows", rows).put("msg", "删除角色成功");
    }
}
