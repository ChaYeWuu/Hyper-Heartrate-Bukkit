package com.chayewuu.hyperheartratebukkit.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * NickPlus 兼容桥接 — 通过 PlaceholderAPI 获取玩家伪装 UUID。
 * <p>
 * 当玩家使用 NickPlus 匿名后，客户端看到的是 Fake UUID，
 * 本工具类通过 {@code %nickplus_fakeuuid%} 变量获取该 UUID，
 * 使 S2C 广播使用 Fake UUID 发送，确保 Fabric 客户端能正确匹配玩家。
 * </p>
 *
 * <p><b>工作原理：</b></p>
 * <ol>
 *   <li>玩家使用 NickPlus 的 /nick 匿名 → 获得 Fake UUID（v5 格式）</li>
 *   <li>客户端看到的是 Fake UUID，但服务端 {@code Player.getUniqueId()} 返回真实 UUID</li>
 *   <li>本工具调用 {@code PlaceholderAPI.setPlaceholders(player, "%nickplus_fakeuuid%")}</li>
 *   <li>若返回有效 UUID，说明玩家已匿名；否则返回 null</li>
 *   <li>消息监听器在广播 S2C 时用 Fake UUID 替换真实 UUID</li>
 * </ol>
 */
public class NickPlusBridge {

    /**
     * 获取玩家的 NickPlus Fake UUID。
     *
     * @param player 目标玩家
     * @return Fake UUID，未匿名或无法获取时返回 null
     */
    public static UUID getFakeUuid(Player player) {
        try {
            String result = PlaceholderAPI.setPlaceholders(player, "%nickplus_fakeuuid%");
            // 未替换时返回原样，NickPlus 未安装时也一样
            if (result == null || result.isEmpty() || result.equals("%nickplus_fakeuuid%")) {
                return null;
            }
            return UUID.fromString(result);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查玩家是否已匿名（有 Fake UUID）。
     *
     * @param player 目标玩家
     * @return {@code true} 表示已匿名
     */
    public static boolean isNicked(Player player) {
        return getFakeUuid(player) != null;
    }
}