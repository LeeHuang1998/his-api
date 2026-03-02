package com.leehuang.his.api.mis.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.mis.dto.banner.request.BannerPageRequest;
import com.leehuang.his.api.mis.dto.banner.request.BannerRequest;
import com.leehuang.his.api.mis.dto.banner.vo.BannerPageVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import com.leehuang.his.api.mis.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@RestController
@RequestMapping("/mis/banner")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    /**
     * 获取轮播图分页数据
     * @param request       分页查询参数
     * @return              分页查询结果
     */
    @PostMapping("/getBannerList")
    @SaCheckPermission(value = {"ROOT", "GOODS:SELECT"}, mode = SaMode.OR)
    public R getBannerList(@RequestBody @Valid BannerPageRequest request) {
        PageUtils<BannerPageVO> pageVO = bannerService.getBannerList(request);
        return R.OK().put("bannerPageData", pageVO);
    }

    /**
     * 更新轮播图状态
     * @param request        更新状态参数
     * @return               更新结果
     */
    @PostMapping("/updateBannerStatus")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R updateBannerStatus(@RequestBody @Valid UpdateStatusRequest request) {
        int i = bannerService.updateBannerStatus(request);
        return R.OK().put("result", i);
    }

    /**
     * 上传轮播图
     * @param file           轮播图文件
     * @return               轮播图路径
     */
    @PostMapping("/uploadBanner")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R uploadBanner(@RequestParam("file") MultipartFile file, @RequestParam("oldPath") String oldPath) {
        String path = bannerService.uploadBanner(file, oldPath);
        return R.OK().put("path", path);
    }

    /**
     * 新增轮播图
     * @param bannerRequest     新增轮播图参数
     * @return                  新增结果
     */
    @PostMapping("/insertBanner")
    @SaCheckPermission(value = {"ROOT", "GOODS:INSERT"}, mode = SaMode.OR)
    public R insertBanner(@RequestBody @Validated(Insert.class) BannerRequest bannerRequest) {
        int i = bannerService.insertBanner(bannerRequest);
        return R.OK().put("result", i);
    }

    /**
     * 根据 id 获取轮播图信息
     * @param request        轮播图 id
     * @return               轮播图信息
     */
    @PostMapping("/getBannerById")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE", "GOODS:SELECT"}, mode = SaMode.OR)
    public R getBannerById(@RequestBody @Valid IdRequest request) {
        BannerPageVO bannerPageVO = bannerService.getBannerById(request);
        return R.OK().put("banner", bannerPageVO);
    }

    /**
     * 更新轮播图数据
     * @param bannerRequest     更新请求参数
     * @return                  更新结果
     */
    @PostMapping("/updateBanner")
    @SaCheckPermission(value = {"ROOT", "GOODS:UPDATE"}, mode = SaMode.OR)
    public R updateBanner(@RequestBody @Validated(Update.class) BannerRequest bannerRequest) {
        int i = bannerService.updateBanner(bannerRequest);
        return R.OK().put("result", i);
    }

    /**
     * 删除轮播图
     * @param request
     * @return
     */
    @PostMapping("/deleteBanner")
    @SaCheckPermission(value = {"ROOT", "GOODS:DELETE"}, mode = SaMode.OR)
    public R deleteGoods(@RequestBody @Valid IdsRequest request) {
        int rows = bannerService.deleteBanner(request.getIds());
        return R.OK().put("rows", rows);
    }
}
