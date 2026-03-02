package com.leehuang.his.api.db.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehuang.his.api.db.entity.BannerEntity;
import com.leehuang.his.api.mis.dto.banner.request.BannerPageRequest;
import com.leehuang.his.api.mis.dto.banner.request.BannerRequest;
import com.leehuang.his.api.mis.dto.banner.vo.BannerPageVO;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 16pro
 * @description 针对表【tb_banner(banner表)】的数据库操作Mapper
 * @createDate 2025-07-15 15:45:32
 * @Entity com.leehuang.his.api.db.entity.BannerDao
 */
public interface BannerDao extends BaseMapper<BannerEntity> {

    // 分页查询 banner 数据
    List<BannerPageVO> getBannerList(@Param("start") Integer start, @Param("request") BannerPageRequest request);

    // 获取数据总数
    long getBannerListCount(@Param("start") Integer start, @Param("request") BannerPageRequest request);

    // 更新轮播图状态
    int updateBannerStatus(UpdateStatusRequest request);

    // 新增轮播图
    int insertBanner(BannerRequest bannerRequest);

    // 根据 ID 查询轮播图
    BannerPageVO getBannerById(@Param("id") Integer id);

    // 更新轮播图
    int updateBanner(BannerRequest bannerRequest);

    // 查询是否可以删除
    Boolean selectCanDelBanner(@Param("ids") Integer[] ids);

    // 获取所有被选中的图片
    List<String> selectImage(@Param("ids") Integer[] ids);

    // 删除轮播图
    int deleteBanner(@Param("ids") Integer[] ids);

    // 获取已发布的轮播图
    List<String> getPublishedBanner();
}
