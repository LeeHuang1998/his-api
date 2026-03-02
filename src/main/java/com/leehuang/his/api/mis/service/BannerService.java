package com.leehuang.his.api.mis.service;

import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.mis.dto.banner.request.BannerPageRequest;
import com.leehuang.his.api.mis.dto.banner.request.BannerRequest;
import com.leehuang.his.api.mis.dto.banner.vo.BannerPageVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BannerService {

    // 获取分页数据
    PageUtils<BannerPageVO> getBannerList(BannerPageRequest request);

    // 更新轮播图状态
    int updateBannerStatus(UpdateStatusRequest request);

    // 上传轮播图
    String uploadBanner(MultipartFile file, String oldPath);

    // 新增轮播图数据
    int insertBanner(BannerRequest bannerRequest);

    // 根据 id 获取轮播图数据
    BannerPageVO getBannerById(IdRequest request);

    // 更新轮播图数据
    int updateBanner(BannerRequest bannerRequest);

    // 删除轮播图
    int deleteBanner(Integer[] ids);

    // 获取已发布的轮播图
    List<String> getPublishedBanner();
}
