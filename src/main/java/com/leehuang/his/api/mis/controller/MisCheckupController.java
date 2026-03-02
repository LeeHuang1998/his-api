package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.mis.dto.checkup.request.CheckupResultRequest;
import com.leehuang.his.api.mis.dto.checkup.request.PlaceCheckupRequest;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.service.MisCheckupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/mis/checkup")
@RequiredArgsConstructor
public class MisCheckupController {

    private final MisCheckupService misCheckupService;

    /**
     * 根据科室查询检查项目
     * @param request
     * @return
     */
    @PostMapping("/searchCheckupByPlace")
    @SaCheckPermission(value = {"ROOT", "CHECKUP:SELECT"}, mode = SaMode.OR)
    public R searchCheckupByPlace(@RequestBody @Valid PlaceCheckupRequest request) {
        PlaceCheckupVO placeCheckupVO = misCheckupService.searchCheckupByPlace(request.getUuid(), request.getPlaceId(), request.getPlace());
        return R.OK().put("checkupResultVO", placeCheckupVO);
    }

    /**
     * 添加检查结果
     * @param request
     * @return
     */
    @PostMapping("/addCheckupResult")
    @SaCheckPermission(value = {"ROOT", "CHECKUP:INSERT", "CHECKUP:UPDATE"}, mode = SaMode.OR)
    public R addCheckupResult(@RequestBody @Valid CheckupResultRequest request) {
        int userId = StpUtil.getLoginIdAsInt();
        NextPlaceVO dto = misCheckupService.addCheckupResult(userId, request);
        if (dto != null) {
            return R.OK().put("msg", "添加体检结果成功").put("nextPlace", dto);
        } else {
            return R.OK().put("msg", "所有体检均已完成！");
        }
    }
}
