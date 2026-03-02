package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.DeptEntity;
import com.leehuang.his.api.mis.dto.dept.vo.DeptPageVO;
import com.leehuang.his.api.mis.dto.dept.vo.DeptDetailVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_dept(部门表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity  com.leehuang.his.api.db.entity.DeptEntity
*/
public interface DeptDao extends BaseMapper<DeptEntity> {

    // 获取所有部门
    List<DeptEntity> getAllDepts();

    // 获取部门总数
    long getDeptCount(@Param("deptName") String deptName);

    // 根据分页条件查询部门数据
    List<DeptPageVO> getDeptsByPage(@Param("start") int start, @Param("length") int length, @Param("deptName") String deptName);

    // 添加新部门
    int insertDept(DeptEntity entity);

    // 根据 id 获取部门信息
    DeptDetailVO getDeptById(@Param("id") int id);

    // 更新部门信息
    int updateDept(DeptEntity entity);

    // 判断部门是否可以被删除，返回 1 表示可以删除，0 表示不可以删除。在批量删除时，若任意一个部门存在员工，本次操作的所有部门都无法删除
    boolean selectCanDelDept(@Param("ids") Integer[] ids);

    // 删除部门
    int deleteDeptByIds(@Param("ids") Integer[] ids);
}




