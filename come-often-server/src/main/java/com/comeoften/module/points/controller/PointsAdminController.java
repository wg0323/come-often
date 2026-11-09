package com.comeoften.module.points.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.common.job.JobResult;
import com.comeoften.job.PointsExpireJob;
import com.comeoften.module.points.dto.MemberLevelSaveDTO;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.service.PointsAdminService;
import com.comeoften.module.points.service.PointsLevelService;
import com.comeoften.module.points.vo.MemberLevelVO;
import com.comeoften.module.points.vo.PointsMemberVO;
import com.comeoften.module.points.vo.PointsRecordVO;
import com.comeoften.module.points.vo.PointsRuleVO;
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
 * PC 后台积分与等级。复用 M1 已有的 member:manage 权限点（菜单「用户管理」），
 * 不新增菜单与种子数据；积分规则参数在 application.yml，页面只读展示。
 */
@Tag(name = "PC 后台-积分与等级", description = "积分规则、等级配置、会员积分与积分流水")
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class PointsAdminController {

    private final PointsAccountService pointsAccountService;

    private final PointsLevelService pointsLevelService;

    private final PointsAdminService pointsAdminService;

    private final PointsExpireJob pointsExpireJob;

    @Operation(summary = "积分规则", description = "发放额度与兑换比例来自配置，只读")
    @ApiResponse(responseCode = "200", description = "返回规则")
    @RequirePerm("member:manage")
    @GetMapping("/rule")
    public Result<PointsRuleVO> rule() {
        return Result.ok(pointsAccountService.rule());
    }

    @Operation(summary = "等级列表", description = "按排序值升序")
    @ApiResponse(responseCode = "200", description = "返回全部等级")
    @RequirePerm("member:manage")
    @GetMapping("/level")
    public Result<List<MemberLevelVO>> levelList() {
        return Result.ok(pointsLevelService.listLevels());
    }

    @Operation(summary = "编辑等级配置", description = "只能改门槛与折扣率；等级不存在返回 30039")
    @ApiResponse(responseCode = "200", description = "返回更新后的等级")
    @RequirePerm("member:manage")
    @PutMapping("/level/{levelId}")
    public Result<MemberLevelVO> updateLevel(
            @Parameter(description = "等级 ID", required = true) @PathVariable Long levelId,
            @Valid @RequestBody MemberLevelSaveDTO dto) {
        return Result.ok(pointsLevelService.updateLevel(levelId, dto));
    }

    @Operation(summary = "会员积分分页", description = "keyword 命中昵称或手机号")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("member:manage")
    @GetMapping("/member/page")
    public Result<PageResult<PointsMemberVO>> memberPage(
            @Parameter(description = "昵称或手机号关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(pointsAdminService.memberPage(current, size, keyword));
    }

    @Operation(summary = "积分流水分页", description = "memberId 为空时查全部会员的流水")
    @ApiResponse(responseCode = "200", description = "返回分页流水")
    @RequirePerm("member:manage")
    @GetMapping("/record/page")
    public Result<PageResult<PointsRecordVO>> recordPage(
            @Parameter(description = "会员 ID，可空") @RequestParam(required = false) Long memberId,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(pointsAdminService.recordPage(memberId, current, size));
    }

    @Operation(summary = "手动执行积分过期",
            description = "运维与验收用；与每日 00:05 批处理共用同一入口与同一把 @JobLock")
    @ApiResponse(responseCode = "200", description = "返回本次过期掉的积分总数；锁被其他实例持有时 data 为 null")
    @RequirePerm("member:manage")
    @PostMapping("/expire/run")
    public Result<Integer> expireRun() {
        JobResult result = pointsExpireJob.expireDue();
        // 保持 M6 验收口径：锁被占用时返回 0（而不是 null），页面与脚本都不用判空
        Integer count = result.getCount() == null ? Integer.valueOf(0) : result.getCount();
        return Result.ok(count);
    }
}
