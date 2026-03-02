package com.leehuang.his.api.front.controller;

import com.leehuang.his.api.common.R;
import com.leehuang.his.api.front.dto.index.vo.FrontIndexVO;
import com.leehuang.his.api.front.service.FrontIndexService;
import com.leehuang.his.api.common.request.IdsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/front/index")
@RequiredArgsConstructor
public class FrontIndexController {

    private final FrontIndexService frontIndexService;

    /**
     * 获取首页数据
     * @param request       商品分区 ids
     * @return              banner 和每个分区的四个商品
     */
    @PostMapping("/getIndexPageData")
    public R getIndexPageData(@RequestBody IdsRequest request){
        FrontIndexVO indexVO = frontIndexService.getIndexPageData(request);
        return R.OK().put("indexPageData",indexVO);
    }
}
