package com.comeoften.module.points.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 会员等级页数据：当前等级 + 全部档位 + 距下一档的差值，差值由服务端算好。
 */
@Data
@Schema(description = "会员等级页数据")
public class MemberLevelOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前等级")
    private MemberLevelVO currentLevel;

    @Schema(description = "全部等级，按排序值升序")
    private List<MemberLevelVO> levels;

    @Schema(description = "本年度已消费金额，单位元")
    private BigDecimal annualAmount;

    @Schema(description = "累计获得积分")
    private Integer totalEarned;

    @Schema(description = "下一个等级；已是最高等级时为空")
    private MemberLevelVO nextLevel;

    @Schema(description = "距下一档还差多少消费额，单位元；无下一档为 0")
    private BigDecimal gapAmount;

    @Schema(description = "距下一档还差多少积分；无下一档为 0")
    private Integer gapPoints;
}
