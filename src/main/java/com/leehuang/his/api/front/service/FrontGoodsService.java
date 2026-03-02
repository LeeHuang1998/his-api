package com.leehuang.his.api.front.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.front.dto.goods.request.GoodsListPageRequest;
import com.leehuang.his.api.front.dto.goods.vo.GoodsPageVO;
import com.leehuang.his.api.front.dto.goods.vo.GoodsSnapshotVO;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;

public interface FrontGoodsService {

    // 根据 id 获取商品页数据
    GoodsPageVO getGoodsFrontPageVoById(int id);

    // 分页查询 goodsList
    PageUtils<GoodsItemVO> searchGoodsListByPage(GoodsListPageRequest request);

    // 获取商品快照
    GoodsSnapshotVO searchSnapshotById(String snapshotId, Integer customerId);
}
