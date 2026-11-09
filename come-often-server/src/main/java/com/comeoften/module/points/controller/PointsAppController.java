package com.comeoften.module.points.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.points.dto.PointsProfileDTO;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.service.PointsLevelService;
import com.comeoften.module.points.vo.MemberLevelOverviewVO;
import com.comeoften.module.points.vo.PointsAccountVO;
import com.comeoften.module.points.vo.PointsRecordVO;
import com.comeoften.module.points.vo.PointsRuleVO;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 顾客端积分。会员 ID 一律取登录态，不接受前端传入；金额与积分抵扣额全部由服务端计算。
 */
@Tag(name = "顾客端-积分", description = "积分明细、签到、分享、完善资料与会员等级")
@RestController
@RequestMapping("/api/app/points")
@RequiredArgsConstructor
public class PointsAppController {

    private final PointsAccountService pointsAccountService;

    private final PointsLevelService pointsLevelService;

    @Operation(summary = "积分账户概览", description = "余额、累计获得与使用、当前会员等级")
    @ApiResponse(responseCode = "200", description = "返回账户概览")
    @GetMapping("/account")
    public Result<PointsAccountVO> account() {
        return Result.ok(pointsAccountService.account(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "积分明细", description = "按发生时间倒序分页")
    @ApiResponse(responseCode = "200", description = "返回分页流水")
    @GetMapping("/record/page")
    public Result<PageResult<PointsRecordVO>> recordPage(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(pointsAccountService.recordPage(LoginUserHolder.getRequired().getUserId(),
                current, size));
    }

    @Operation(summary = "每日签到", description = "每人每天一次，重复签到返回 30036")
    @ApiResponse(responseCode = "200", description = "返回签到后的账户，earnedPoints 为本次获得积分")
    @PostMapping("/sign-in")
    public Result<PointsAccountVO> signIn() {
        return Result.ok(pointsAccountService.signIn(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "每日分享", description = "每人每天一次，重复分享返回 30037")
    @ApiResponse(responseCode = "200", description = "返回分享后的账户，earnedPoints 为本次获得积分")
    @PostMapping("/share")
    public Result<PointsAccountVO> share() {
        return Result.ok(pointsAccountService.share(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "完善资料", description = "更新昵称；手机号是登录账号，只能通过微信手机号授权变更。积分终身只发一次，重复返回 30038")
    @ApiResponse(responseCode = "200", description = "返回完善后的账户")
    @PostMapping("/profile")
    public Result<PointsAccountVO> completeProfile(@Valid @RequestBody PointsProfileDTO dto) {
        return Result.ok(pointsAccountService.completeProfile(LoginUserHolder.getRequired().getUserId(),
                dto));
    }

    @Operation(summary = "会员等级", description = "当前等级、全部档位、本年度消费额与距下一档的差值")
    @ApiResponse(responseCode = "200", description = "返回等级页数据")
    @GetMapping("/level")
    public Result<MemberLevelOverviewVO> level() {
        return Result.ok(pointsLevelService.overview(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "积分规则", description = "页面展示用，额度与兑换比例全部来自服务端配置")
    @ApiResponse(responseCode = "200", description = "返回规则")
    @GetMapping("/rule")
    public Result<PointsRuleVO> rule() {
        return Result.ok(pointsAccountService.rule());
    }
}
