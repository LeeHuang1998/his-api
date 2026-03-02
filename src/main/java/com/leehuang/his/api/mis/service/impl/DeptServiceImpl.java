package com.leehuang.his.api.mis.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.DeptEntity;
import com.leehuang.his.api.db.dao.DeptDao;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.dept.request.DeptPageRequest;
import com.leehuang.his.api.mis.dto.dept.request.DeptRequest;
import com.leehuang.his.api.mis.dto.dept.vo.DeptPageVO;
import com.leehuang.his.api.mis.dto.dept.vo.DeptDetailVO;
import com.leehuang.his.api.mis.service.DeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service("deptService")
public class DeptServiceImpl implements DeptService {

    @Resource
    private DeptDao deptDao;

    // 获取所有部门
    @Override
    public List<DeptEntity> getAllDepts() {
        return deptDao.getAllDepts();
    }

    /**
     * 分页查询部门
     * @param request       前端查询参数
     * @return              根据分页条件查询的部门数据，包装为 PageUtils 对象
     */
    @Override
    public PageUtils<DeptPageVO> getDeptsByPage(DeptPageRequest request) {
        // 创建 List 存储查询的数据
        List<DeptPageVO> deptsByPage = new ArrayList<>();

        // 获取分页条件
        int length = request.getLength();
        int page = request.getPage();

        // 获取部门总数
        long deptCount = deptDao.getDeptCount(request.getDeptName());

        if (deptCount != 0){
            // 每页数据开始的 id
            int start = (page - 1) * length;
            deptsByPage = deptDao.getDeptsByPage(start, length, request.getDeptName());
        }

        return new PageUtils<>(deptCount, length, page, deptsByPage);
    }

    /**
     * 添加新部门
     * @param request   新部门参数
     * @return          添加成功返回 1，否则返回 0
     */
    @Override
    @Transactional
    public int insertDept(DeptRequest request) {
        DeptEntity deptEntity = new DeptEntity();
        BeanUtil.copyProperties(request, deptEntity);
        return deptDao.insertDept(deptEntity);
    }

    /**
     * 根据 id 获取部门信息
     * @param id     部门 id
     * @return       部门信息
     */
    @Override
    public DeptDetailVO getDeptById(int id) {
        return deptDao.getDeptById(id);
    }

    /**
     * 更新部门信息
     * @param request    部门信息
     * @return           更新成功返回 1，否则返回 0
     */
    @Override
    @Transactional
    public int updateDept(DeptRequest request) {
        // 转换为实体类
        DeptEntity deptEntity = new DeptEntity();
        BeanUtil.copyProperties(request, deptEntity);
        return deptDao.updateDept(deptEntity);
    }

    /**
     * 删除部门
     * @param ids       传入的部门 id 数组
     * @return          删除的行数
     */
    @Override
    @Transactional
    public int deleteDeptByIds(Integer[] ids) {
        // 判断是否可以删除
        if (deptDao.selectCanDelDept(ids)){
            return deptDao.deleteDeptByIds(ids);
        } else {
            throw new HisException("批量删除的部门中有部门仍有在职员工，无法删除");
        }
    }
}
