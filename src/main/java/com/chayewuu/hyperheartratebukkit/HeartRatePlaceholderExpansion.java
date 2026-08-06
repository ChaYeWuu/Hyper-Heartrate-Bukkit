package com.chayewuu.hyperheartratebukkit;

import com.chayewuu.hyperheartratebukkit.network.RemoteHeartRateStore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * PlaceholderAPI 变量扩展 — 提供玩家心率 Placeholder 变量。
 *
 * <p><b>注册的变量：</b></p>
 * <table>
 *   <tr><th>变量</th><th>说明</th><th>示例</th></tr>
 *   <tr><td>{@code %hyperheartrate_heartrate%}</td><td>当前玩家自身心率</td><td>72</td></tr>
 *   <tr><td>{@code %hyperheartrate_heartrate_<玩家名>%}</td><td>指定玩家的心率</td><td>85</td></tr>
 *   <tr><td>{@code %hyperheartrate_heartrate_color%}</td><td>带颜色代码的心率（根据范围变色）</td><td>&#x26a0;&#xfe0f;72</td></tr>
 *   <tr><td>{@code %hyperheartrate_heartrate_color_<玩家名>%}</td><td>指定玩家的带色心率</td><td>&#x26a0;&#xfe0f;85</td></tr>
 *   <tr><td>{@code %hyperheartrate_status%}</td><td>当前玩家心率状态</td><td>有数据 / 无数据</td></tr>
 *   <tr><td>{@code %hyperheartrate_status_<玩家名>%}</td><td>指定玩家心率状态</td><td>有数据</td></tr>
 * </table>
 *
 * <p><b>颜色规则：</b></p>
 * <ul>
 *   <li>&lt; 60 BPM：蓝色 (&amp;b)</li>
 *   <li>60–100 BPM：绿色 (&amp;a)</li>
 *   <li>100–140 BPM：黄色 (&amp;e)</li>
 *   <li>&gt; 140 BPM：红色 (&amp;c)</li>
 * </ul>
 */
public class HeartRatePlaceholderExpansion extends PlaceholderExpansion {

    private final HeartRateBukkitPlugin plugin;
    private final RemoteHeartRateStore store;

    public HeartRatePlaceholderExpansion(HeartRateBukkitPlugin plugin) {
        this.plugin = plugin;
        this.store = RemoteHeartRateStore.getInstance();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hyperheartrate";
    }

    @Override
    public @NotNull String getAuthor() {
        return "茶叶Wuu";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // 插件重载时保持注册
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // 解析参数：heartrate / heartrate_<玩家名> / heartrate_color / heartrate_color_<玩家名> / status / status_<玩家名>
        String targetPlayerName = player.getName();
        boolean colored = false;
        boolean statusOnly = false;

        String param = params.trim();

        if (param.startsWith("heartrate_color_")) {
            colored = true;
            targetPlayerName = param.substring("heartrate_color_".length());
        } else if (param.startsWith("heartrate_color")) {
            colored = true;
            // 使用当前玩家
        } else if (param.startsWith("heartrate_")) {
            targetPlayerName = param.substring("heartrate_".length());
        } else if (param.startsWith("status_")) {
            statusOnly = true;
            targetPlayerName = param.substring("status_".length());
        } else if (param.equals("heartrate")) {
            // 使用当前玩家
        } else if (param.equals("status")) {
            statusOnly = true;
        } else {
            return null; // 未知变量
        }

        // 获取目标玩家 UUID
        UUID targetUuid = getPlayerUuid(targetPlayerName);
        if (targetUuid == null) {
            return statusOnly ? "§7离线" : "0";
        }

        int heartRate = store.getHeartRate(targetUuid);

        if (statusOnly) {
            return heartRate > 0 ? "§a有数据" : "§7无数据";
        }

        if (heartRate <= 0) {
            return colored ? "§7--" : "0";
        }

        if (colored) {
            return getColorCode(heartRate) + heartRate;
        }

        return String.valueOf(heartRate);
    }

    /**
     * 根据心率值返回对应颜色代码。
     */
    private String getColorCode(int heartRate) {
        if (heartRate < 60) return "§b";   // 蓝色
        if (heartRate <= 100) return "§a"; // 绿色
        if (heartRate <= 140) return "§e"; // 黄色
        return "§c";                        // 红色
    }

    /**
     * 根据玩家名获取 UUID。
     *
     * @param playerName 玩家名
     * @return UUID，未找到时返回 null
     */
    private @Nullable UUID getPlayerUuid(String playerName) {
        Player target = plugin.getServer().getPlayerExact(playerName);
        if (target != null) {
            return target.getUniqueId();
        }
        // 尝试模糊匹配（离线时可能用到）
        Player matched = plugin.getServer().getPlayer(playerName);
        if (matched != null) {
            return matched.getUniqueId();
        }
        return null;
    }
}