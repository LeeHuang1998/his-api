package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.goods.request.GoodsListPageRequest;
import com.leehuang.his.api.front.dto.goods.request.SearchGoodsSnapshotByIdRequest;
import com.leehuang.his.api.front.dto.goods.vo.GoodsSnapshotVO;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.front.service.FrontGoodsService;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.front.dto.goods.vo.GoodsPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/front/goods")
@RequiredArgsConstructor
public class FrontGoodsController {

    private final FrontGoodsService frontGoodsService;

    /**
     * 根据 id 获取商品详情
     * @param request   商品 id
     * @return
     */
    @PostMapping("/getGoodsById")
    public R getGoodsFrontPageVoById(@RequestBody @Valid IdRequest request) {
        GoodsPageVO pageVO = frontGoodsService.getGoodsFrontPageVoById(request.getId());
        return R.OK().put("pageVO", pageVO);
    }

    /**
     * 获取商品分页数据
     * @param request
     * @return
     */
    @PostMapping("/searchGoodsListByPage")
    public R searchGoodsListByPage(@RequestBody @Valid GoodsListPageRequest request) {
        PageUtils<GoodsItemVO> goodsListByPage = frontGoodsService.searchGoodsListByPage(request);
        return R.OK().put("pageData", goodsListByPage);
    }

    /**
     * mis 端获取商品快照
     * @param request   快照 id
     * @return
     */
    @PostMapping("/searchSnapshotForMis")
    @SaCheckLogin
    public R searchSnapshotForMis(@RequestBody @Valid SearchGoodsSnapshotByIdRequest request){
        GoodsSnapshotVO snapshot = frontGoodsService.searchSnapshotById(request.getSnapshotId(), null);
        return R.OK().put("snapshot", snapshot);
    }

    /**
     * front 端获取生怕快照
     * @param form
     * @return
     */
    @PostMapping("/searchSnapshotForFront")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R searchSnapshotForFront(@RequestBody @Valid SearchGoodsSnapshotByIdRequest form) {
        int customerId = StpCustomerUtil.getLoginIdAsInt();
        GoodsSnapshotVO snapshot = frontGoodsService.searchSnapshotById(form.getSnapshotId(), customerId);
        return R.OK().put("snapshot", snapshot);
    }
}
