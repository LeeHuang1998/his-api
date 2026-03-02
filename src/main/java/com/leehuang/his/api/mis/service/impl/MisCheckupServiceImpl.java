package com.leehuang.his.api.mis.service.impl;

import com.leehuang.his.api.db.dao.CheckupResultDao;
import com.leehuang.his.api.db.pojo.CheckupResultEntity;
import com.leehuang.his.api.mis.service.MisFlowRegulationService;
import com.leehuang.his.api.mis.dto.checkup.request.CheckupResultRequest;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupResultVO;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;
import com.leehuang.his.api.mis.dto.goods.vo.CheckupVO;
import com.leehuang.his.api.mis.service.MisCheckupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service("MisCheckupService")
@RequiredArgsConstructor
@Slf4j
public class MisCheckupServiceImpl implements MisCheckupService {

    private final CheckupResultDao checkupResultDao;

    private final MisFlowRegulationService misFlowRegulationService;

    /**
     * 根据科室查询体检项目
     * @param uuid
     * @param placeId
     * @param place
     * @return
     */
    @Override
    public PlaceCheckupVO searchCheckupByPlace(String uuid, Integer placeId, String place) {
        PlaceCheckupVO placeCheckupVO = new PlaceCheckupVO();

        // 1. 根据科室查询体检项目
        CheckupResultEntity resultEntity = checkupResultDao.searchResultByUuid(uuid);

        // 2. 过滤出该科室的所有体检项目
        List<CheckupVO> checkupVOList = resultEntity.getCheckup().stream()
                .filter(checkup -> checkup.getPlace().trim().equals(place)).collect(Collectors.toList());

        placeCheckupVO.setCheckupVOList(checkupVOList);

        // 3. 判断该科室是否已经为该体检录入过体检结果
        List<PlaceCheckupResultVO> resultVOList = resultEntity.getResult().stream()
                .filter(result -> Objects.equals(result.getPlaceId(), placeId)).collect(Collectors.toList());

        boolean resultIsEmpty = resultVOList.isEmpty();

        // 4. 如果科室已经提交过检查结果
        if (!resultIsEmpty) {
            // 检查结果非空时，返回 checkupResultList
            placeCheckupVO.setCheckupResultList(resultVOList);
        }

        // 设置是否已经体检过检查结果，若非空则为提交过 true，否则为 false
        placeCheckupVO.setHasAlreadyCheckup(!resultIsEmpty);

        return placeCheckupVO;
    }

    /**
     * 往 mongodb 中添加检查结果
     * @param userId
     * @param request
     */
    @Override
    public NextPlaceVO addCheckupResult(int userId, CheckupResultRequest request) {
        // 构建体检结果对象
        PlaceCheckupResultVO placeCheckupResultVO = new PlaceCheckupResultVO();
        placeCheckupResultVO.setPlaceId(request.getPlaceId());
        placeCheckupResultVO.setPlace(request.getPlace());
        placeCheckupResultVO.setDoctorName(request.getDoctorName());
        placeCheckupResultVO.setTemplate(request.getTemplate());

        placeCheckupResultVO.setDoctorId(userId);
        placeCheckupResultVO.setCheckupDate(LocalDate.now());
        placeCheckupResultVO.setCheckupItems(request.getItem());

        // 保存到 mongodb 中
        checkupResultDao.addCheckupResult(request.getUuid(), placeCheckupResultVO);

        // 获取并返回推荐的科室
        return misFlowRegulationService.finishPlaceAndRecommendNext(request.getUuid(), request.getCustomerName(), request.getPlace(), request.getPlaceId());
    }
}
