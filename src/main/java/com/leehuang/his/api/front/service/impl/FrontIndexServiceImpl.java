package com.leehuang.his.api.front.service.impl;

import com.alibaba.fastjson.JSON;
import com.leehuang.his.api.config.properties.MinioProperties;
import com.leehuang.his.api.front.dto.index.vo.FrontIndexVO;
import com.leehuang.his.api.front.dto.index.vo.GoodsItemVO;
import com.leehuang.his.api.front.service.FrontIndexService;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.mis.service.BannerService;
import com.leehuang.his.api.mis.service.MisGoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("FrontIndexService")
@RequiredArgsConstructor
public class FrontIndexServiceImpl implements FrontIndexService {

    private final BannerService bannerService;

    private final MisGoodsService misGoodsService;

    private final MinioProperties minioProperties;

    @Override
    public FrontIndexVO getIndexPageData(IdsRequest request) {
        // 获取 banner 数据
        List<String> bannerList = bannerService.getPublishedBanner().stream().map(banner -> {
            banner = minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + banner;
            return banner;
        }).collect(Collectors.toList());

        // 获取分区商品数据
        List<GoodsItemVO> partGoods = misGoodsService.getGoodsByPartIds(request.getIds()).stream().peek(goods -> {
            // 将 image 转换为数组，并添加地址
            String image = goods.getImage();
            image = minioProperties.getEndpoint() + "/" + minioProperties.getBucket() + "/" + JSON.parseObject(image, String[].class)[0];
            goods.setImage(image);
        }).collect(Collectors.toList());

        // 组装数据
        return FrontIndexVO.builder().bannerList(bannerList).partGoods(partGoods).build();
    }
}
