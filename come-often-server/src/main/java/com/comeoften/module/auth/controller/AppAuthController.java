package com.comeoften.module.auth.controller;

import com.comeoften.common.Result;
import com.comeoften.module.auth.dto.AppLoginDTO;
import com.comeoften.module.auth.service.AppAuthService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MemberInfoVO;
import com.comeoften.security.LoginUser;
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

/** 顾客端登录、退出与当前会员信息。 */
@Tag(name = "顾客端-认证", description = "手机号登录换取顾客端 JWT；其余 /api/app/** 接口均需该 token")
@RestController
@RequestMapping("/api/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final AppAuthService appAuthService;

    @Operation(summary = "手机号登录", description = "getPhoneNumber 的 code 换手机号，按手机号 upsert 会员，签发有效期 1 天的顾客端 JWT")
    @ApiResponse(responseCode = "200", description = "登录成功，返回 token 与有效期")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody AppLoginDTO dto) {
        return Result.ok(appAuthService.login(dto));
    }

    @Operation(summary = "退出登录", description = "删除 Redis 中的当前有效凭证，token 立即失效")
    @ApiResponse(responseCode = "200", description = "退出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        appAuthService.logout(LoginUserHolder.getRequired());
        return Result.ok();
    }

    @Operation(summary = "当前会员信息", description = "需顾客端 token；手机号脱敏返回，可用于验收 401")
    @ApiResponse(responseCode = "200", description = "返回当前会员信息")
    @ApiResponse(responseCode = "401", description = "未登录或登录已失效")
    @GetMapping("/info")
    public Result<MemberInfoVO> info() {
        LoginUser loginUser = LoginUserHolder.getRequired();
        return Result.ok(appAuthService.info(loginUser.getUserId()));
    }
}
