package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.mis.dto.rule.request.RulePageRequest;
import com.leehuang.his.api.mis.dto.rule.request.RuleRequest;
import com.leehuang.his.api.mis.dto.rule.vo.RulePageVO;
import com.leehuang.his.api.mis.dto.rule.vo.RuleVO;
import com.leehuang.his.api.mis.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/mis/rule")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    /**
     * 获取分页数据
     * @param request
     * @return
     */
    @PostMapping("/searchRuleDataByPage")
    @SaCheckPermission(value = {"ROOT", "RULE:SELECT"}, mode = SaMode.OR)
    public R searchRuleDataByPage(@RequestBody @Valid RulePageRequest request) {
        PageUtils<RulePageVO> pageData = ruleService.searchRuleDataByPage(request);
        return R.OK().put("pageData", pageData);
    }

    /**
     * 插入新规则
     * @param request
     * @return
     */
    @PostMapping("/insertRule")
    @SaCheckPermission(value = {"ROOT", "RULE:INSERT"}, mode = SaMode.OR)
    public R insert(@RequestBody @Validated(Insert.class) RuleRequest request) {
        int rows = ruleService.insertRule(request);
        return R.OK().put("rows", rows);
    }

    /**
     * 根据 id 查询规则
     * @param request
     * @return
     */
    @PostMapping("/searchRuleById")
    @SaCheckPermission(value = {"ROOT", "RULE:SELECT"}, mode = SaMode.OR)
    public R searchById(@RequestBody @Valid IdRequest request) {
        RuleVO pageVO = ruleService.searchRuleById(request.getId());
        return R.OK().put("pageVO", pageVO);
    }


    /**
     * 更新规则
     * @param request
     * @return
     */
    @PostMapping("/updateRule")
    @SaCheckPermission(value = {"ROOT", "RULE:UPDATE"}, mode = SaMode.OR)
    public R update(@RequestBody @Validated(Update.class) RuleRequest request) {
        int rows = ruleService.updateRule(request);
        return R.OK().put("rows", rows);
    }

    /**
     * 删除规则
     * @param request
     * @return
     */
    @PostMapping("/deleteRuleById")
    @SaCheckPermission(value = {"ROOT", "RULE:DELETE"}, mode = SaMode.OR)
    public R deleteById(@RequestBody @Valid IdRequest request) {
        int rows = ruleService.deleteRuleById(request.getId());
        return R.OK().put("rows", rows);
    }
}
