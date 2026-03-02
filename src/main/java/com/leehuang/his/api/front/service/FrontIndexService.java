package com.leehuang.his.api.front.service;

import com.leehuang.his.api.front.dto.index.vo.FrontIndexVO;
import com.leehuang.his.api.common.request.IdsRequest;

public interface FrontIndexService {

    FrontIndexVO getIndexPageData(IdsRequest request);
}
