package com.leehuang.his.api.mis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.entity.FlowRegulationEntity;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationPageRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.FlowRegulationPageVO;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.PlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.RealTimeQueueDataVO;

import java.util.List;

public interface MisFlowRegulationService extends IService<FlowRegulationEntity> {

    // 获取人员调流页面科室列表
    List<PlaceVO> searchPlaceList();

    // 获取调流模式，判断是否为自动调流模式
    boolean searchFlowRegulationMode(String item);

    // 获取调流页面分页数据
    PageUtils<FlowRegulationPageVO> searchFlowRegulationPage(FlowRegulationPageRequest request);

    // 插入新的调流规则
    int insertFlowRegulation(FlowRegulationRequest request);

    // 更新调流规则
    int updateFlowRegulation(FlowRegulationRequest request);

    // 根据 id 查询调流规则数据
    FlowRegulationPageVO searchFlowRegulationById(Integer id);

    // 完成体检，并获取下一个检查科室
    NextPlaceVO finishPlaceAndRecommendNext(String uuid, String customerName, String finishedPlace, Integer placeId);

    // 获取推荐的科室
    NextPlaceVO recommendNextPlace(String uuid, Boolean toPlaceQueue);

    // 获取所有科室实时排队人数
    List<RealTimeQueueDataVO> searchRealTimeQueueData();

    // 根据科室 id，获取该科室实时排队人员名单
    List<String> searchQueueByPlace(Integer id);

    // 给指定科室添加排队人员
    Boolean addQueuePerson(Integer id, String uuid);

    // 科室中跳过指定的排队人员
    Boolean skipQueuePerson(Integer id, String uuid);

    // 删除或批量删除科室调流规则
    int deleteFlowRegulation(Integer[] ids);

    // 停用指定科室
    int deactivatePlace(Integer id);

    // 启用指定科室
    int enablePlace(Integer id);

    // 修改调流模式
    int changeFlowRegulationMode(String value);

}
