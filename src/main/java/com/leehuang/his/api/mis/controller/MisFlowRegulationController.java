package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.PlaceVO;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.RealTimeQueueDataVO;
import com.leehuang.his.api.mis.dto.system.request.SystemRequest;
import com.leehuang.his.api.mis.service.MisFlowRegulationService;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationPageRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.request.FlowRegulationRequest;
import com.leehuang.his.api.mis.dto.flowRegulation.vo.FlowRegulationPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@RequestMapping("/mis/flowRegulation")
@RequiredArgsConstructor
public class MisFlowRegulationController {

    private final MisFlowRegulationService misFlowRegulationService;

    /**
     * 获取人员调流页面科室列表
     * @return
     */
    @GetMapping("/searchPlaceList")
    @SaCheckLogin
    public R searchPlaceList() {
        List<PlaceVO> placeList = misFlowRegulationService.searchPlaceList();
        return R.OK().put("placeList", placeList);
    }

    /**
     * 获取当前调流模式，判断是否为自动调流模式
     * @return
     */
    @GetMapping("/searchFlowRegulationMode")
    @SaCheckLogin
    public R searchFlowRegulationMode() {
        boolean mode = misFlowRegulationService.searchFlowRegulationMode("auto_flow_regulation");
        return R.OK().put("mode", mode);
    }

    /**
     * 获取人员调流页面分页数据
     * @param request   查询分页数据请求参数
     * @return
     */
    @PostMapping("/searchFlowRegulationPage")
    @SaCheckLogin
    public R searchFlowRegulationPage(@RequestBody @Valid FlowRegulationPageRequest request) {
        PageUtils<FlowRegulationPageVO> pageVOS = misFlowRegulationService.searchFlowRegulationPage(request);
        return R.OK().put("pageData", pageVOS);
    }

    /**
     * 新增人员调流规则
     * @param request   新增调流规则的具体参数
     * @return
     */
    @PostMapping("/insert")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:INSERT"}, mode = SaMode.OR)
    public R insertFlowRegulation(@RequestBody @Validated(Insert.class) FlowRegulationRequest request) {
        int rows = misFlowRegulationService.insertFlowRegulation(request);
        return R.OK().put("result", rows == 1);
    }

    /**
     * 根据 id 查询调流规则数据
     * @param id
     * @return
     */
    @GetMapping("/searchFlowRegulationById/{id}")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:SELECT"}, mode = SaMode.OR)
    public R searchFlowRegulationById(@PathVariable @Min(value = 1) Integer id) {
        FlowRegulationPageVO pageVO = misFlowRegulationService.searchFlowRegulationById(id);
        return R.OK().put("pageVO", pageVO);
    }

    /**
     * 更新人员调流规则
     * @param request   更新调流规则的具体参数
     * @return
     */
    @PostMapping("/update")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:UPDATE"}, mode = SaMode.OR)
    public R updateFlowRegulation(@RequestBody @Validated(Update.class) FlowRegulationRequest request) {
        int rows = misFlowRegulationService.updateFlowRegulation(request);
        return R.OK().put("result", rows == 1);
    }

    /**
     * 继续体检时，推荐下一个前往的体检科室
     * @param uuid
     * @return
     */
    @GetMapping("/getRecommendNextPlace/{uuid}")
    @SaCheckLogin
    public R getRecommendNextPlace(@PathVariable String uuid) {
        // 继续体检时，下一个科室为还未完成的科室，需要排队，所以传入 true
        NextPlaceVO nextPlaceVO = misFlowRegulationService.recommendNextPlace(uuid, true);
        return R.OK().put("nextPlace", nextPlaceVO);
    }

    /**
     * 获取所有科室实时排队人数
     * @return
     */
    @GetMapping("/searchRealTimeQueueData")
    public R searchRealTimeQueueData() {
        List<RealTimeQueueDataVO> dataVOList = misFlowRegulationService.searchRealTimeQueueData();
        return R.OK().put("realTimeQueueDataVOList", dataVOList);
    }

    /**
     * 根据科室 id，获取该科室实时排队人员名单
     * @param id
     * @return
     */
    @GetMapping("/searchQueueByPlace/{id}")
    public R searchQueueByPlace(@PathVariable Integer id) {
        List<String> placeQueue = misFlowRegulationService.searchQueueByPlace(id);
        return R.OK().put("placeQueue", placeQueue);
    }

    /**
     * 给指定科室挂号
     * @param id
     * @param uuid
     * @return
     */
    @GetMapping("/addQueuePerson/{id}/{uuid}")
    @SaCheckLogin
    public R addQueuePerson(@PathVariable Integer id, @PathVariable String uuid) {
        Boolean result =  misFlowRegulationService.addQueuePerson(id, uuid);
        return R.OK().put("result", result);
    }

    @GetMapping("/skipQueuePerson/{id}/{uuid}")
    @SaCheckLogin
    public R skipQueuePerson(@PathVariable Integer id, @PathVariable String uuid) {
        Boolean result =  misFlowRegulationService.skipQueuePerson(id, uuid);
        return R.OK().put("result", result);
    }

    /**
     * 停用指定科室
     * @param id    科室 id
     * @return
     */
    @PatchMapping("/deactivatePlace/{id}")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:UPDATE"}, mode = SaMode.OR)
    public R deactivatePlace(@PathVariable Integer id) {
        int rows = misFlowRegulationService.deactivatePlace(id);
        return R.OK().put("rows", rows);
    }

    /**
     * 启用指定科室
     * @param id
     * @return
     */
    @PatchMapping("/enablePlace/{id}")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:UPDATE"}, mode = SaMode.OR)
    public R enablePlace(@PathVariable Integer id) {
        int rows = misFlowRegulationService.enablePlace(id);
        return R.OK().put("rows", rows);
    }

    /**
     * 删除或批量删除科室调流规则
     * @param idsRequest    id 集合
     * @return
     */
    @DeleteMapping("/deleteFlowRegulation")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:DELETE"}, mode = SaMode.OR)
    public R deleteFlowRegulation(@RequestBody @Valid IdsRequest idsRequest) {
        int rows = misFlowRegulationService.deleteFlowRegulation(idsRequest.getIds());
        return R.OK().put("rows", rows);
    }

    /**
     * 修改调流模式
     * @param request
     * @return
     */
    @PatchMapping("/changeFlowRegulationMode")
    @SaCheckPermission(value = {"ROOT", "FLOW_REGULATION:UPDATE"}, mode = SaMode.OR)
    public R changeFlowRegulationMode(@RequestBody SystemRequest request) {
        int rows = misFlowRegulationService.changeFlowRegulationMode(request.getValue());
        if (rows == 1) {
            return R.OK().put("msg", "修改调流模式成功");
        } else {
            return R.error("修改调流模式失败");
        }
    }
}
