package com.leehuang.his.api.mis.service.impl;

import com.leehuang.his.api.common.utils.MinioUtil;
import com.leehuang.his.api.common.utils.PageUtils;
import com.leehuang.his.api.db.dao.BannerDao;
import com.leehuang.his.api.db.dao.GoodsDao;
import com.leehuang.his.api.exception.BizCodeEnum;
import com.leehuang.his.api.exception.HisException;
import com.leehuang.his.api.mis.dto.banner.request.BannerPageRequest;
import com.leehuang.his.api.mis.dto.banner.request.BannerRequest;
import com.leehuang.his.api.mis.dto.banner.vo.BannerPageVO;
import com.leehuang.his.api.common.request.IdRequest;
import com.leehuang.his.api.common.request.UpdateStatusRequest;
import com.leehuang.his.api.mis.service.BannerService;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerServiceImpl implements BannerService {

    private final BannerDao bannerDao;

    private final GoodsDao goodsDao;

    private final MinioUtil minioUtil;

    /**
     * 获取分页数据
     * @param request       分页查询请求，包含关键词、状态和分页参数
     * @return
     */
    @Override
    public PageUtils<BannerPageVO> getBannerList(BannerPageRequest request) {
        Integer page = request.getPage();
        // 获取从第几条记录开始的 index
        Integer start = (page - 1) * request.getLength();

        // 获取分页数据
        List<BannerPageVO> bannerList = bannerDao.getBannerList(start, request);
        long totalCount = bannerDao.getBannerListCount(start, request);

        return new PageUtils<>(totalCount, request.getLength(),request.getPage(),bannerList);
    }

    /**
     * 更新轮播图状态
     * @param request    更新状态请求，包含轮播图 ID、goodsID 和 轮播图状态
     * @return           更新行数
     */
    @Override
    @Transactional
    public int updateBannerStatus(UpdateStatusRequest request) {
        // 获取商品状态
        Boolean status = goodsDao.selectStatus(request.getId());
        if (status) {
            // 更新状态
            return bannerDao.updateBannerStatus(request);
        } else {
            throw new HisException("商品未上架，无法发布轮播图");
        }
    }

    /**
     * 上传轮播图
     * @param file          上传文件
     * @param oldPath       原来的 banner 路径（update banner 时）
     * @return              上传路径
     */
    @Override
    @Transactional
    public String uploadBanner(MultipartFile file, String oldPath) {
        // 1. 校验上传文件
        if (file == null || file.isEmpty()) {
            throw new HisException("上传文件不能为空");
        }

        // 2. 校验文件名
        String fileName = file.getOriginalFilename();
        // 增加文件名非空校验
        if (fileName == null || fileName.isEmpty()) {
            throw new HisException("文件名无效");
        }

        // 3. 删除旧图片
        if (oldPath != null && !oldPath.isEmpty()) {
            if (oldPath.startsWith("front/banner/")) {
                try {
                    minioUtil.removeFile(oldPath);
                    log.debug("删除旧 banner 成功：{}", oldPath);
                } catch (Exception e) {
                    // 删除失败不影响新图片上传，仅记录日志
                    log.error("删除旧 banner 失败（将保留旧文件）：{}", oldPath, e);
                }
            } else {
                log.warn("旧 banner 路径格式不合法，跳过删除：{}", oldPath);
            }
        }

        // 4. 生成新图片路径并上传
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        String path = "front/banner/" + UUID.randomUUID().toString().replace("-", "") + extension;
        minioUtil.uploadImage(path, file);

        // 5. 返回新图片路径
        return path;
    }

    /**
     * 新增轮播图
     * @param bannerRequest     新增轮播图请求，包含轮播图名称、轮播图路径、轮播图状态和对应商品 ID
     * @return                  新增行数
     */
    @Override
    @Transactional
    public int insertBanner(BannerRequest bannerRequest) {
        try {
            return bannerDao.insertBanner(bannerRequest);
        } catch (DuplicateKeyException e) {
            throw new HisException("绑定的套餐 id 已经绑定其他轮播图");
        }
    }

    /**
     *
     * @param request
     * @return
     */
    @Override
    public BannerPageVO getBannerById(IdRequest request) {
        return bannerDao.getBannerById(request.getId());
    }

    /**
     * 更新轮播图信息
     * @param bannerRequest     更新轮播图请求，包含 id、轮播图名称、轮播图路径、轮播图状态和对应商品 ID
     * @return                  更新行数
     */
    @Override
    @Transactional
    public int updateBanner(BannerRequest bannerRequest) {
        try {
            return bannerDao.updateBanner(bannerRequest);
        } catch (DuplicateKeyException e) {
            throw new HisException("绑定的套餐 id 已经绑定其他轮播图");
        }
    }

    /**
     * 删除轮播图
     * @param ids        轮播图 id 数组
     * @return           删除行数
     */
    @Override
    @Transactional
    public int deleteBanner(Integer[] ids) {
        Boolean canDel = bannerDao.selectCanDelBanner(ids);

        if (canDel) {
            // 1. 查询对应的图片
            List<String> imagePathList = bannerDao.selectImage(ids);

            // 2. 从 minio 中批量删除图片
            List<DeleteObject> deleteObjList
                    = imagePathList.stream().map(DeleteObject::new).collect(Collectors.toList());
            minioUtil.removeFiles(deleteObjList);

            // 3. 更新数据库数据，删除所有的商品
            return bannerDao.deleteBanner(ids);
        } else {
            throw new HisException(BizCodeEnum.HIS_EXCEPTION.getCode(), "存在不满足被删除条件的 banner，无法删除");
        }
    }

    /**
     * 获取所有的已发布的 banner
     * @return
     */
    @Override
    public List<String> getPublishedBanner() {
        return bannerDao.getPublishedBanner();
    }

}