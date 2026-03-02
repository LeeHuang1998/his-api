package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.db.entity.DeptEntity;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.mis.dto.dept.request.DeptPageRequest;
import com.leehuang.his.api.mis.dto.dept.request.DeptRequest;
import com.leehuang.his.api.mis.dto.dept.vo.DeptPageVO;
import com.leehuang.his.api.mis.dto.dept.vo.DeptDetailVO;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import com.leehuang.his.api.mis.service.DeptService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/mis/dept")
public class DeptController {
    @Resource
    private DeptService deptService;

    // 获取所有部门
    @GetMapping("/getAllDepts")
    public R getAllDepts(){
        List<DeptEntity> allDepts = deptService.getAllDepts();
        return R.OK().put("deptList", allDepts);
    }

    /**
     * 分页查询部门数据
     * @param request  查询参数
     * @return         根据分页条件查询的部门数据
     */
    @PostMapping("/getDeptListByPage")
    @SaCheckPermission(value = {"ROOT", "DEPT:SELECT"}, mode = SaMode.OR)
    public R getDeptListByPage(@RequestBody @Valid DeptPageRequest request){
        // 根据条件查询
        PageUtils<DeptPageVO> pageUtils = deptService.getDeptsByPage(request);
        return R.OK().put("deptPageData", pageUtils);
    }

    /**
     * 添加部门
     * @param request   添加部门请求参数
     * @return          执行结果，成功返回 1
     */
    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT", "DEPT:INSERT"}, mode = SaMode.OR)
    public R insert(@RequestBody @Validated({Insert.class}) DeptRequest request){
        int rows = deptService.insertDept(request);
        if (rows == 1){
            return R.OK().put("rows", rows).put("msg", "添加部门成功");
        }
        return R.OK().put("rows", rows).put("msg", "添加部门失败");
    }

    /**
     * 根据id查询部门信息
     * @param request   查询参数
     * @return          查询到的部门信息
     */
    @PostMapping("/getDeptById")
    @SaCheckPermission(value = {"ROOT", "DEPT:UPDATE"}, mode = SaMode.OR)
    public R getDeptById(@RequestBody @Valid IdRequest request){
        DeptDetailVO deptById = deptService.getDeptById(request.getId());
        return R.OK().put("deptInfo", deptById);
    }

    /**
     * 更新部门信息
     * @param request   更新参数
     * @return          执行结果，成功返回 1
     */
    @PostMapping("update")
    @SaCheckPermission(value = {"ROOT", "DEPT:UPDATE"}, mode = SaMode.OR)
    public R update(@RequestBody @Validated({Update.class}) DeptRequest request){
        int rows = deptService.updateDept(request);
        if (rows == 1){
            return R.OK().put("rows", rows).put("msg", "更新部门信息成功");
        }
        return R.OK().put("rows", rows).put("msg", "更新部门信息失败");
    }

    /**
     * 批量删除部门
     * @param request    批量删除请求参数
     * @return           执行结果，成功返回删除的行数
     */
    @PostMapping("/delete")
    @SaCheckPermission(value = {"ROOT", "DEPT:DELETE"}, mode = SaMode.OR)
    public R delete(@RequestBody @Valid IdsRequest request) {
        int rows = deptService.deleteDeptByIds(request.getIds());
        return R.OK().put("rows", rows).put("msg", "删除成功");
    }
}
