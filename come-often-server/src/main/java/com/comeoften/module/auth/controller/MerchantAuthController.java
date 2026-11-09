package com.comeoften.module.auth.controller;

import com.comeoften.common.Result;
import com.comeoften.module.auth.dto.MerchantLoginDTO;
import com.comeoften.module.auth.dto.WxLoginDTO;
import com.comeoften.module.auth.service.StaffAuthService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.UserInfoVO;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** 商家端首次账号绑定与后续免密登录。 */
@Tag(name = "商家端-认证与权限", description = "首次账号密码 + 微信 code 绑定 openid，此后凭 openid 免密登录")
@RestController
@RequestMapping("/api/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final StaffAuthService staffAuthService;

    @Operation(summary = "首次登录并绑定微信", description = "校验员工账号密码后把 openid 写入 sys_user，签发商家端 JWT")
    @ApiResponse(responseCode = "200", description = "登录并绑定成功")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody MerchantLoginDTO dto) {
        return Result.ok(staffAuthService.merchantLogin(dto));
    }

    @Operation(summary = "静默免密登录", description = "按 wx.login 换取 openid 查员工；未绑定或已解绑返回 401")
    @ApiResponse(responseCode = "200", description = "换发商家端 JWT")
    @ApiResponse(responseCode = "401", description = "未绑定员工账号或微信绑定已解除")
    @PostMapping("/auto-login")
    public Result<LoginVO> autoLogin(@Valid @RequestBody WxLoginDTO dto) {
        return Result.ok(staffAuthService.merchantAutoLogin(dto));
    }

    @Operation(summary = "当前员工信息", description = "返回角色与权限点，商家端宫格按 permissions 渲染")
    @ApiResponse(responseCode = "200", description = "返回当前员工信息")
    @ApiResponse(responseCode = "401", description = "未登录或登录已失效")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(staffAuthService.info(LoginUserHolder.getRequired()));
    }

    @Operation(summary = "退出登录", description = "删除 Redis 中的当前有效凭证，需重新输入账号密码绑定")
    @ApiResponse(responseCode = "200", description = "退出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        staffAuthService.logout(LoginUserHolder.getRequired());
        return Result.ok();
    }
}
