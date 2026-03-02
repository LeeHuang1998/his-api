package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.mis.dto.checkup.request.CheckupResultRequest;
import com.leehuang.his.api.mis.dto.checkup.vo.PlaceCheckupVO;
import com.leehuang.his.api.mis.dto.flowRegulation.dto.NextPlaceVO;

public interface MisCheckupService {
    PlaceCheckupVO searchCheckupByPlace(String uuid, Integer placeId, String place);

    // 往 mongodb 中添加检查结果
    NextPlaceVO addCheckupResult(int userId, CheckupResultRequest request);
}
