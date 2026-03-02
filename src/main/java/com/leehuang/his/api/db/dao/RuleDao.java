package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.RuleEntity;
import com.leehuang.his.api.mis.dto.rule.request.RulePageRequest;
import com.leehuang.his.api.mis.dto.rule.vo.RulePageVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author 16pro
* @description 针对表【tb_rule(规则表)】的数据库操作Mapper
* @createDate 2025-07-15 15:45:32
* @Entity com.leehuang.his.api.db.entity.RuleEntity
*/
public interface RuleDao extends BaseMapper<RuleEntity> {

    // 获取 rule 模块分页信息
    List<RulePageVO> searchRuleDataByPage(@Param("start") int start, @Param("request") RulePageRequest request);

    // 获取 rule 模块分页信息总数
    int searchRuleDataCount(@Param("request") RulePageRequest request);

}




