package com.comeoften.module.stats.controller;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.Result;
import com.comeoften.module.stats.service.StatsExportService;
import com.comeoften.module.stats.service.StatsService;
import com.comeoften.module.stats.vo.StatsDashboardVO;
import com.comeoften.module.stats.vo.StatsExportFile;
import com.comeoften.module.stats.vo.StatsReportVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * PC 后台数据看板与报表。两个页面两个权限点：
 * 经营看板走 dashboard:view（菜单 100），数据报表与导出走 stats:view（菜单 106）。
 * 全部只读，不新增菜单与权限点。
 */
@Tag(name = "PC 后台-数据看板与报表", description = "经营看板、区间报表与 Excel 导出")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class StatsAdminController {

    private static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final StatsService statsService;

    private final StatsExportService statsExportService;

    @Operation(summary = "经营看板",
            description = "今日净营收 / 订单数 / 客单价、今日预约看板与今日热销前 5；净营收已扣退款")
    @ApiResponse(responseCode = "200", description = "返回经营看板数据")
    @RequirePerm("dashboard:view")
    @GetMapping("/dashboard")
    public Result<StatsDashboardVO> dashboard() {
        return Result.ok(statsService.dashboard(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "数据报表",
            description = "一个接口返回整页：区间汇总、营收趋势、热销排行、券 ROI、复购留存与高价值用户")
    @ApiResponse(responseCode = "200", description = "返回报表整页")
    @RequirePerm("stats:view")
    @GetMapping("/report")
    public Result<StatsReportVO> report(
            @Parameter(description = "起始日 yyyy-MM-dd，默认结束日往前 6 天")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日 yyyy-MM-dd，含当天，默认今天")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "时间粒度 DAY / WEEK / MONTH，默认 DAY")
            @RequestParam(required = false) String granularity,
            @Parameter(description = "热销与高价值用户取前 N 条，默认 10，上限 50")
            @RequestParam(required = false) Integer topN) {
        return Result.ok(statsService.report(LoginUserHolder.getRequired().getStoreId(),
                parseDate(startDate), parseDate(endDate), granularity, topN));
    }

    @Operation(summary = "导出报表 Excel",
            description = "按报表类型导出 xlsx；行数超过 300 行返回业务码 30056，不返回截断文件")
    @ApiResponse(responseCode = "200", description = "返回 xlsx 文件流")
    @RequirePerm("stats:view")
    @GetMapping("/export")
    public void export(
            @Parameter(description = "导出类型 trend / hot-dish / coupon-roi / high-value-member")
            @RequestParam(required = false) String type,
            @Parameter(description = "起始日 yyyy-MM-dd，默认结束日往前 6 天")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日 yyyy-MM-dd，含当天，默认今天")
            @RequestParam(required = false) String endDate,
            @Parameter(description = "时间粒度 DAY / WEEK / MONTH，仅趋势导出使用")
            @RequestParam(required = false) String granularity,
            @Parameter(description = "热销与高价值用户取前 N 条，默认 10，上限 50")
            @RequestParam(required = false) Integer topN,
            HttpServletResponse response) throws IOException {
        StatsExportFile file = statsExportService.export(LoginUserHolder.getRequired().getStoreId(),
                type, parseDate(startDate), parseDate(endDate), granularity, topN);
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding("UTF-8");
        // 中文文件名走 filename*，同时给一个 ASCII 兜底，旧下载器不认 RFC 5987 时不会丢扩展名
        response.setHeader("Content-Disposition", "attachment; filename=\"report.xlsx\"; filename*=UTF-8''"
                + URLEncoder.encode(file.getFileName(), "UTF-8"));
        response.getOutputStream().write(file.getContent());
        response.flushBuffer();
    }

    /**
     * 日期按字符串收再手动解析：改成 LocalDate 绑定后格式错误会落到通用异常，
     * 前端只能看到「参数不合法」，分不清是区间非法还是日期写错。
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.STATS_DATE_RANGE_INVALID, "日期格式必须为 yyyy-MM-dd");
        }
    }
}
