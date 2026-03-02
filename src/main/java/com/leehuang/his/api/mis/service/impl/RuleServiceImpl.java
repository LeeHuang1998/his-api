package com.leehuang.his.api.mis.service.impl;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.dao.RuleDao;
import com.leehuang.his.api.db.entity.RuleEntity;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.rule.request.RulePageRequest;
import com.leehuang.his.api.mis.dto.rule.request.RuleRequest;
import com.leehuang.his.api.mis.dto.rule.vo.RulePageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RuleVO;
import com.leehuang.his.api.mis.service.MisGoodsService;
import com.leehuang.his.api.mis.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("ruleService")
@RequiredArgsConstructor
public class RuleServiceImpl implements RuleService {

    private final RuleDao ruleDao;

    private final MisGoodsService misGoodsService;

    /**
     * 获取分页数据
     * @param request
     * @return
     */
    @Override
    public PageUtils<RulePageVO> searchRuleDataByPage(RulePageRequest request) {

        int page = request.getPage();
        int length = request.getLength();
        int start = (page - 1) * length;

        List<RulePageVO> rulePageVOList = ruleDao.searchRuleDataByPage(start, request);
        int totalCount = ruleDao.searchRuleDataCount(request);

        return new PageUtils<>(totalCount,length, page, rulePageVOList);
    }

    /**
     * 插入新规则
     * @param request
     * @return
     */
    @Transactional
    @Override
    public int insertRule(RuleRequest request) {
        RuleEntity ruleEntity = new RuleEntity();
        ruleEntity.setRule(request.getRule());
        ruleEntity.setName(request.getName());
        if (!request.getRemark().isEmpty()) {
            ruleEntity.setRemark(request.getRemark());
        }

        return ruleDao.insert(ruleEntity);
    }

    /**
     * 根据 id 查询规则
     * @param id
     * @return
     */
    @Override
    public RuleVO searchRuleById(Integer id) {
        RuleEntity ruleEntity = ruleDao.selectById(id);

        RuleVO ruleVO = new RuleVO();
        ruleVO.setId(ruleEntity.getId());
        ruleVO.setRuleName(ruleEntity.getName());
        ruleVO.setRule(ruleEntity.getRule());
        ruleVO.setRemark(ruleEntity.getRemark());

        return ruleVO;
    }

    /**
     * 更新规则
     * @param request
     * @return
     */
    @Transactional
    @Override
    public int updateRule(RuleRequest request) {
        RuleEntity ruleEntity = new RuleEntity();
        ruleEntity.setId(request.getId());
        ruleEntity.setRule(request.getRule());
        ruleEntity.setName(request.getName());

        if (!request.getRemark().isEmpty()) {
            ruleEntity.setRemark(request.getRemark());
        }

        return ruleDao.updateById(ruleEntity);
    }

    /**
     * 删除规则
     * @param id
     * @return
     */
    @Override
    public int deleteRuleById(Integer id) {
        // 先查询规则是否有关联商品
        Long count = misGoodsService.searchGoodsCountByRuleId(id);
        if (count > 0) {
            throw new HisException("该规则下存在关联商品，无法删除");
        }
        return ruleDao.deleteById(id);
    }
}
