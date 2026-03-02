package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.DeptEntity;
import com.leehuang.his.api.mis.dto.dept.request.DeptPageRequest;
import com.leehuang.his.api.mis.dto.dept.request.DeptRequest;
import com.leehuang.his.api.mis.dto.dept.vo.DeptPageVO;
import com.leehuang.his.api.mis.dto.dept.vo.DeptDetailVO;

import java.util.List;

public interface DeptService {

    // 获取所有部门
    List<DeptEntity> getAllDepts();

    // 分页查询部门
    PageUtils<DeptPageVO> getDeptsByPage(DeptPageRequest request);

    // 添加部门
    int insertDept(DeptRequest request);

    // 根据 id 获取部门信息
    DeptDetailVO getDeptById(int id);

    // 更新部门信息
    int updateDept(DeptRequest request);

    // 删除部门
    int deleteDeptByIds(Integer[] ids);
}
