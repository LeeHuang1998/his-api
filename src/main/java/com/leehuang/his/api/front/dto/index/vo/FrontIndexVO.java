package com.leehuang.his.api.front.dto.index.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FrontIndexVO {

    private List<String> bannerList;

    private List<GoodsItemVO> partGoods;
}
