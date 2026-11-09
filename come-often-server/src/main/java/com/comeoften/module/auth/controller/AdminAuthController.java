package com.comeoften.module.auth.controller;

import com.comeoften.common.Result;
import com.comeoften.module.auth.dto.AccountLoginDTO;
import com.comeoften.module.auth.service.StaffAuthService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MenuVO;
import com.comeoften.module.auth.vo.UserInfoVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
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
import java.util.List;

/** PC 后台登录、续期、菜单与权限点。 */
@Tag(name = "PC 后台-认证与权限", description = "员工账号 BCrypt 登录，JWT 有效期 2 小时；菜单按角色返回")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final StaffAuthService staffAuthService;

    @Operation(summary = "员工账号登录", description = "校验 BCrypt 密码与门店归属，签发后台 JWT")
    @ApiResponse(responseCode = "200", description = "登录成功；账号或密码错误时 body.code=40104")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody AccountLoginDTO dto) {
        return Result.ok(staffAuthService.adminLogin(dto));
    }

    @Operation(summary = "当前员工信息", description = "返回角色与权限点，PC 菜单按 permissions 渲染")
    @ApiResponse(responseCode = "200", description = "返回当前员工信息")
    @ApiResponse(responseCode = "401", description = "未登录或登录已失效")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.ok(staffAuthService.info(LoginUserHolder.getRequired()));
    }

    @Operation(summary = "可见菜单", description = "只返回该员工角色拥有的目录与页面，按 sort 排序")
    @ApiResponse(responseCode = "200", description = "返回菜单列表")
    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.ok(staffAuthService.menus(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "续期", description = "旧 token 仍有效时换发新 token 并刷新 Redis TTL")
    @ApiResponse(responseCode = "200", description = "返回新 token")
    @PostMapping("/refresh")
    public Result<LoginVO> refresh() {
        return Result.ok(staffAuthService.refresh(LoginUserHolder.getRequired()));
    }

    @Operation(summary = "退出登录", description = "删除 Redis 中的当前有效凭证，token 立即失效")
    @ApiResponse(responseCode = "200", description = "退出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        staffAuthService.logout(LoginUserHolder.getRequired());
        return Result.ok();
    }

    @Operation(summary = "权限校验探针", description = "仅演示 @RequirePerm：需要 system:user，其余角色返回 403")
    @ApiResponse(responseCode = "200", description = "拥有该权限")
    @ApiResponse(responseCode = "403", description = "无该权限")
    @RequirePerm("system:user")
    @GetMapping("/permission-probe")
    public Result<String> permissionProbe() {
        return Result.ok("system:user granted");
    }
}
