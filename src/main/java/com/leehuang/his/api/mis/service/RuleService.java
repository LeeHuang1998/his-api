package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.mis.dto.rule.request.RulePageRequest;
import com.leehuang.his.api.mis.dto.rule.request.RuleRequest;
import com.leehuang.his.api.mis.dto.rule.vo.RulePageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RuleVO;

public interface RuleService {
    // mis 端 rule 模块分页数据
    PageUtils<RulePageVO> searchRuleDataByPage(RulePageRequest request);

    // 插入新规则
    int insertRule(RuleRequest request);

    // 根据 id 查询规则
    RuleVO searchRuleById(Integer id);

    // 更新规则
    int updateRule(RuleRequest request);

    // 根据 id 删除规则
    int deleteRuleById(Integer id);
}
