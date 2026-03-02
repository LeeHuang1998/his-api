package com.leehuang.his.api.front.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.leehuang.his.api.common.R;
import com.leehuang.his.api.config.sa_token.StpCustomerUtil;
import com.leehuang.his.api.front.dto.address.request.AddressRequest;
import com.leehuang.his.api.front.dto.address.request.AddressStatusRequest;
import com.leehuang.his.api.front.dto.address.vo.AddressVO;
import com.leehuang.his.api.front.service.AddressService;
import com.leehuang.his.api.common.request.IdsRequest;
import com.leehuang.his.api.common.validation.Insert;
import com.leehuang.his.api.common.validation.Update;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/front/customer/mine/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 添加或更新地址信息
     * @return
     */
    @PostMapping("/insertAddress")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R insertAddress(@RequestBody @Validated(Insert.class) AddressRequest request) {
        addressService.saveAddress(request);
        return R.OK("保存地址成功");
    }

    /**
     * 修改地址信息
     * @param request
     * @return
     */
    @PutMapping("/updateAddress")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R updateAddress(@RequestBody @Validated(Update.class) AddressRequest request) {
        addressService.saveAddress(request);
        return R.OK("修改地址成功");
    }

    /**
     * 设置默认地址
     * @param request
     * @return
     */
    @PatchMapping("/updateAddressDefault")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R updateAddressDefault(@RequestBody @Valid AddressStatusRequest request) {
        addressService.updateAddressDefault(request);
        return R.OK("设置地址状态成功");
    }

    /**
     * 删除地址信息
     * @param idsRequest
     * @return
     */
    @DeleteMapping("/deleteAddress")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R deleteAddress(@RequestBody @Valid IdsRequest idsRequest) {
        int rows = addressService.deleteAddress(idsRequest.getIds());
        return R.OK("删除地址成功，共删除 " + rows + " 条数据").put("rows", rows);
    }


    @GetMapping("/addressList")
    @SaCheckLogin(type = StpCustomerUtil.TYPE)
    public R getAddressListByCustomerId() {
        List<AddressVO> addressList = addressService.getAddressListByCustomerId();
        return R.OK().put("addressList", addressList);
    }
}
