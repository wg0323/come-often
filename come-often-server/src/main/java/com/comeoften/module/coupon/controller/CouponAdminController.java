package com.comeoften.module.coupon.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.coupon.constant.VerifyChannel;
import com.comeoften.module.coupon.dto.CouponGrantDTO;
import com.comeoften.module.coupon.dto.CouponGrantRuleSaveDTO;
import com.comeoften.module.coupon.dto.CouponTemplateSaveDTO;
import com.comeoften.module.coupon.dto.CouponVerifyDTO;
import com.comeoften.module.coupon.service.CouponAdminService;
import com.comeoften.module.coupon.service.CouponRuleService;
import com.comeoften.module.coupon.service.CouponVerifyService;
import com.comeoften.module.coupon.vo.CouponGrantRuleVO;
import com.comeoften.module.coupon.vo.CouponRoiVO;
import com.comeoften.module.coupon.vo.CouponTemplateVO;
import com.comeoften.module.coupon.vo.CouponUseRecordVO;
import com.comeoften.module.coupon.vo.CouponVerifyVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * PC 后台营销管理。核销接口与 M9 商家端共用 CouponVerifyService，
 * 这里只把入口标记为 ADMIN_INPUT（收银台输码 / 扫码枪）。
 */
@Tag(name = "PC 后台-营销管理", description = "券模板、定向发券、核销台、核销记录与券 ROI")
@RestController
@RequestMapping("/api/admin/coupon")
@RequiredArgsConstructor
public class CouponAdminController {

    private final CouponAdminService couponAdminService;

    private final CouponVerifyService couponVerifyService;

    private final CouponRuleService couponRuleService;

    @Operation(summary = "券模板分页", description = "按名称模糊与状态过滤")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("coupon:manage")
    @GetMapping("/template/page")
    public Result<PageResult<CouponTemplateVO>> pageTemplate(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "券名称关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：0-草稿，1-发放中，2-已停用") @RequestParam(required = false) Integer status) {
        return Result.ok(couponAdminService.pageTemplate(
                LoginUserHolder.getRequired().getStoreId(), current, size, keyword, status));
    }

    @Operation(summary = "券模板详情")
    @ApiResponse(responseCode = "200", description = "返回券模板")
    @RequirePerm("coupon:manage")
    @GetMapping("/template/{templateId}")
    public Result<CouponTemplateVO> detailTemplate(
            @Parameter(description = "券模板 ID", required = true) @PathVariable Long templateId) {
        return Result.ok(couponAdminService.detailTemplate(
                LoginUserHolder.getRequired().getStoreId(), templateId));
    }

    @Operation(summary = "新建券模板", description = "5 类券按 couponType 校验面额 / 折扣率 / 指定菜品")
    @ApiResponse(responseCode = "200", description = "返回新建的券模板")
    @RequirePerm("coupon:manage")
    @PostMapping("/template")
    public Result<CouponTemplateVO> createTemplate(@Valid @RequestBody CouponTemplateSaveDTO dto) {
        return Result.ok(couponAdminService.createTemplate(
                LoginUserHolder.getRequired().getStoreId(), dto));
    }

    @Operation(summary = "编辑券模板", description = "已领取量不被覆盖：新余量 = 新总量 − 已领取量")
    @ApiResponse(responseCode = "200", description = "返回更新后的券模板")
    @RequirePerm("coupon:manage")
    @PutMapping("/template/{templateId}")
    public Result<CouponTemplateVO> updateTemplate(
            @Parameter(description = "券模板 ID", required = true) @PathVariable Long templateId,
            @Valid @RequestBody CouponTemplateSaveDTO dto) {
        return Result.ok(couponAdminService.updateTemplate(
                LoginUserHolder.getRequired().getStoreId(), templateId, dto));
    }

    @Operation(summary = "切换券模板状态", description = "0-草稿，1-发放中，2-已停用；提交后重置 Redis 库存键")
    @ApiResponse(responseCode = "200", description = "返回更新后的券模板")
    @RequirePerm("coupon:manage")
    @PostMapping("/template/{templateId}/status")
    public Result<CouponTemplateVO> updateStatus(
            @Parameter(description = "券模板 ID", required = true) @PathVariable Long templateId,
            @Parameter(description = "目标状态", required = true) @RequestParam Integer status) {
        return Result.ok(couponAdminService.updateStatus(
                LoginUserHolder.getRequired().getStoreId(), templateId, status));
    }

    @Operation(summary = "定向发券", description = "走与领券完全相同的库存链路，返回实际发出的张数")
    @ApiResponse(responseCode = "200", description = "返回发放张数")
    @RequirePerm("coupon:manage")
    @PostMapping("/grant")
    public Result<Integer> grant(@Valid @RequestBody CouponGrantDTO dto) {
        return Result.ok(couponAdminService.grant(LoginUserHolder.getRequired().getStoreId(), dto));
    }

    @Operation(summary = "券核销", description = "PC 输码 / 扫码枪入口；已使用返回 30026，码过期返回 30028")
    @ApiResponse(responseCode = "200", description = "返回券面信息与抵扣金额")
    @RequirePerm("coupon:verify")
    @PostMapping("/verify")
    public Result<CouponVerifyVO> verify(@Valid @RequestBody CouponVerifyDTO dto) {
        return Result.ok(couponVerifyService.verify(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), dto, VerifyChannel.ADMIN_INPUT));
    }

    @Operation(summary = "核销记录分页", description = "按核销时间倒序，带核销员工姓名供审计")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("coupon:record")
    @GetMapping("/record/page")
    public Result<PageResult<CouponUseRecordVO>> pageRecord(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(couponAdminService.pageUseRecord(
                LoginUserHolder.getRequired().getStoreId(), current, size));
    }

    @Operation(summary = "券 ROI", description = "颁发量、领取数、使用数、用券订单数、抵扣与实收金额、投产比")
    @ApiResponse(responseCode = "200", description = "返回 ROI 列表")
    @RequirePerm("coupon:manage")
    @GetMapping("/roi")
    public Result<List<CouponRoiVO>> roi(
            @Parameter(description = "券模板 ID；不传返回全部") @RequestParam(required = false) Long templateId) {
        return Result.ok(couponAdminService.roi(LoginUserHolder.getRequired().getStoreId(), templateId));
    }

    @Operation(summary = "券发放规则列表", description = "新客 / 生日 / 复购 / 沉睡唤醒四条规则的配置与开关")
    @ApiResponse(responseCode = "200", description = "返回规则列表")
    @RequirePerm("coupon:manage")
    @GetMapping("/rule")
    public Result<List<CouponGrantRuleVO>> ruleList() {
        return Result.ok(couponRuleService.list(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "保存券发放规则",
            description = "ruleCode 同门店唯一，重复提交即编辑；ruleType 不合法或复购阈值小于 2 返回 10001，"
                    + "券模板不属于本店返回 30022")
    @ApiResponse(responseCode = "200", description = "返回保存后的规则")
    @RequirePerm("coupon:manage")
    @PostMapping("/rule")
    public Result<CouponGrantRuleVO> saveRule(@Valid @RequestBody CouponGrantRuleSaveDTO dto) {
        return Result.ok(couponRuleService.save(LoginUserHolder.getRequired().getStoreId(), dto));
    }

    @Operation(summary = "启用 / 停用券发放规则")
    @ApiResponse(responseCode = "200", description = "返回更新后的规则")
    @RequirePerm("coupon:manage")
    @PostMapping("/rule/{ruleId}/status")
    public Result<CouponGrantRuleVO> updateRuleStatus(
            @Parameter(description = "规则 ID", required = true) @PathVariable Long ruleId,
            @Parameter(description = "是否启用", required = true) @RequestParam Boolean enabled) {
        return Result.ok(couponRuleService.setEnabled(LoginUserHolder.getRequired().getStoreId(),
                ruleId, enabled));
    }

    @Operation(summary = "手动执行券发放规则",
            description = "只对 BIRTHDAY / SLEEPING 有效（新客与复购由支付 / 完成事件驱动，返回 0）；"
                    + "与每日 00:40 的 coupon-rule 任务同一条链路，重复执行不重复发券")
    @ApiResponse(responseCode = "200", description = "返回本次发出的券张数")
    @RequirePerm("coupon:manage")
    @PostMapping("/rule/run")
    public Result<Integer> runRule(
            @Parameter(description = "触发类型：BIRTHDAY / SLEEPING", required = true)
            @RequestParam String ruleType) {
        return Result.ok(couponRuleService.run(LoginUserHolder.getRequired().getStoreId(), ruleType));
    }

    @Operation(summary = "补发券过期提醒", description = "运维 / 验收用：发一条短延迟消息验证延迟链路与消费者")
    @ApiResponse(responseCode = "200", description = "补发成功")
    @RequirePerm("coupon:manage")
    @PostMapping("/expire-probe")
    public Result<Void> expireProbe(
            @Parameter(description = "用户券 ID", required = true) @RequestParam Long userCouponId,
            @Parameter(description = "延迟秒数，1~3600") @RequestParam(defaultValue = "3") Long delaySeconds) {
        couponAdminService.resendExpireRemind(LoginUserHolder.getRequired().getStoreId(),
                userCouponId, delaySeconds);
        return Result.ok();
    }
}
