package com.comeoften.module.ops.controller;

import com.comeoften.common.Result;
import com.comeoften.common.job.JobResult;
import com.comeoften.job.service.JobTriggerService;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 兜底任务运维入口：列任务名、手动补跑一次。
 *
 * <p>复用 {@code order:manage} 权限点，不新增权限点与菜单。补跑与 cron 走同一个
 * {@link JobTriggerService}，因此共享 @JobLock；另一个实例正在跑时返回 {@code executed=false}，
 * 不会出现两次真实执行。</p>
 */
@Tag(name = "PC 后台-定时任务", description = "兜底任务清单与手动补跑")
@RestController
@RequestMapping("/api/admin/job")
@RequiredArgsConstructor
public class JobAdminController {

    private final JobTriggerService jobTriggerService;

    @Operation(summary = "兜底任务清单", description = "返回已注册的任务名，可作为手动补跑的入参")
    @ApiResponse(responseCode = "200", description = "返回任务名列表")
    @RequirePerm("order:manage")
    @GetMapping
    public Result<List<String>> list() {
        return Result.ok(jobTriggerService.names());
    }

    @Operation(summary = "手动补跑兜底任务",
            description = "锁被其他实例持有时返回 executed=false；任务名不存在返回 10001")
    @ApiResponse(responseCode = "200", description = "返回本次执行结果")
    @RequirePerm("order:manage")
    @PostMapping("/{jobName}/run")
    public Result<JobResult> run(
            @Parameter(description = "任务名，取清单接口返回值", required = true) @PathVariable String jobName) {
        return Result.ok(jobTriggerService.run(jobName));
    }
}
