package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 储值域配置。
 *
 * <p>充值赠送规则按「满额档位」配置在 {@code comeoften.balance.recharge-tiers}，
 * 形如 {@code 500:50,1000:120,2000:300}（充值满 500 送 50，依次类推），取不超过充值金额的最高档。
 * M14 B3 不新增赠送规则表：一期只有三档，配置比建表更好维护；档位格式非法时启动期就抛错，
 * 不允许带着坏配置把「充值不送」当正常行为。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.balance")
public class BalanceProperties {

    /** 单笔充值下限，单位元。 */
    private BigDecimal minRecharge = new BigDecimal("1.00");

    /** 单笔充值上限，单位元。 */
    private BigDecimal maxRecharge = new BigDecimal("5000.00");

    /** 赠送档位原文，{充值金额}:{赠送金额} 逗号分隔。 */
    private String rechargeTiers = "500:50,1000:120,2000:300";

    /**
     * 解析后的赠送档位，按充值门槛升序。首次访问时解析一次并缓存。
     */
    public List<GiftTier> giftTiers() {
        if (rechargeTiers == null || rechargeTiers.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<GiftTier> tiers = new ArrayList<>();
        for (String raw : rechargeTiers.split(",")) {
            String item = raw.trim();
            if (item.isEmpty()) {
                continue;
            }
            String[] parts = item.split(":");
            if (parts.length != 2) {
                throw new IllegalStateException("comeoften.balance.recharge-tiers 格式非法：" + item);
            }
            try {
                tiers.add(new GiftTier(new BigDecimal(parts[0].trim()), new BigDecimal(parts[1].trim())));
            } catch (NumberFormatException e) {
                throw new IllegalStateException("comeoften.balance.recharge-tiers 数值非法：" + item, e);
            }
        }
        tiers.sort((left, right) -> left.getAmount().compareTo(right.getAmount()));
        return tiers;
    }

    /**
     * 赠送档位。金额与赠送额都按两位小数处理。
     */
    @Data
    public static class GiftTier {

        private final BigDecimal amount;

        private final BigDecimal gift;

        public GiftTier(BigDecimal amount, BigDecimal gift) {
            this.amount = amount;
            this.gift = gift;
        }
    }
}
