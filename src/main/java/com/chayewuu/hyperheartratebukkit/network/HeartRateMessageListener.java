package com.chayewuu.hyperheartratebukkit.network;

import com.chayewuu.hyperheartratebukkit.HeartRateBukkitPlugin;
import com.chayewuu.hyperheartratebukkit.integration.NickPlusBridge;
import com.chayewuu.hyperheartratebukkit.util.VarIntUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 心率数据消息监听器 — 中转玩家心率数据。
 * <p>
 * 通过 Bukkit 插件消息通道接收客户端（Fabric 模组）发送的心率数据，
 * 然后广播给附近安装了模组的玩家。
 * </p>
 *
 * <p><b>协议对齐：</b></p>
 * <ul>
 *   <li><b>C2S 通道</b> {@code hyper-heartrate:hr_c2s} — 客户端→服务端：{@code VarInt(heartRate)}</li>
 *   <li><b>S2C 通道</b> {@code hyper-heartrate:hr_s2c} — 服务端→客户端：{@code UUID(playerUuid) + VarInt(heartRate)}</li>
 * </ul>
 *
 * <p>与 Fabric 模组的 {@code MultiplayerNetworking} 协议完全一致。</p>
 */
public class HeartRateMessageListener implements PluginMessageListener {

    /** C2S 通道 — 客户端发送心率到服务端 */
    public static final String CHANNEL_C2S = "hyper-heartrate:hr_c2s";
    /** S2C 通道 — 服务端广播心率给其他玩家 */
    public static final String CHANNEL_S2C = "hyper-heartrate:hr_s2c";

    private final HeartRateBukkitPlugin plugin;
    private final RemoteHeartRateStore store;

    public HeartRateMessageListener(HeartRateBukkitPlugin plugin) {
        this.plugin = plugin;
        this.store = RemoteHeartRateStore.getInstance();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player sender, byte @NotNull [] message) {
        if (!CHANNEL_C2S.equals(channel)) {
            return;
        }

        try {
            // 解析 C2S 数据：VarInt(heartRate)
            VarIntUtil.VarIntResult result = VarIntUtil.decodeVarInt(message, 0);
            int heartRate = result.value();

            UUID senderUuid = sender.getUniqueId();
            plugin.getLogger().fine("收到心率数据: " + sender.getName() + " = " + heartRate + " BPM");

            // 存入本地存储（始终使用真实 UUID）
            store.updateHeartRate(senderUuid, heartRate);

            // 检测 NickPlus 匿名身份：如果玩家使用了 Fake UUID，广播时用 Fake UUID 发送
            UUID broadcastUuid = senderUuid;
            UUID fakeUuid = NickPlusBridge.getFakeUuid(sender);
            if (fakeUuid != null) {
                broadcastUuid = fakeUuid;
                plugin.getLogger().fine("检测到 NickPlus 匿名: " + sender.getName()
                        + " 真实 UUID=" + senderUuid + " → Fake UUID=" + fakeUuid);
            }

            // 广播给附近安装了模组的玩家（使用 broadcastUuid，NickPlus 匿名时用 Fake UUID）
            broadcastToNearbyPlayers(sender, broadcastUuid, heartRate);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "解析心率数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 广播心率数据给附近安装了模组的玩家。
     * <p>
     * 仅向 64 格范围内且注册了 S2C 通道的玩家发送数据，
     * 不阻塞未安装模组的玩家。
     * 与 Fabric 模组的服务端广播逻辑一致。
     * </p>
     *
     * @param sender     数据发送者
     * @param senderUuid 发送者 UUID
     * @param heartRate  心率值
     */
    private void broadcastToNearbyPlayers(Player sender, UUID broadcastUuid, int heartRate) {
        if (heartRate <= 0) {
            return;
        }

        // 构造 S2C 数据：UUID(16字节) + VarInt(heartRate)
        byte[] s2cPayload = buildS2CPayload(broadcastUuid, heartRate);

        // 判断是否使用了 NickPlus Fake UUID
        boolean isNicked = !broadcastUuid.equals(sender.getUniqueId());

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player other : players) {
            // 非匿名玩家：不发给自己（Fabric 原版逻辑，避免重复处理）
            // 匿名玩家：也发给自己，因为客户端看到自己的 Fake UUID 需要匹配
            if (!isNicked && other == sender) {
                continue;
            }

            // 距离检查（64 格内，使用平方距离避免开方）
            if (!other.getWorld().equals(sender.getWorld())) {
                continue;
            }
            if (other.getLocation().distanceSquared(sender.getLocation()) > 64 * 64) {
                continue;
            }

            // 仅向注册了 S2C 通道的玩家发送（即安装了模组/插件的客户端）
            other.sendPluginMessage(plugin, CHANNEL_S2C, s2cPayload);
        }
    }

    /**
     * 构建 S2C 心跳数据包。
     * <p>格式：{@code UUID(16字节大端序) + VarInt(heartRate)}</p>
     *
     * @param playerUuid 玩家 UUID
     * @param heartRate  心率值
     * @return 编码后的字节数组
     */
    private byte[] buildS2CPayload(UUID playerUuid, int heartRate) {
        byte[] uuidBytes = VarIntUtil.uuidToBytes(playerUuid);
        byte[] hrBytes = VarIntUtil.encodeVarInt(heartRate);

        byte[] payload = new byte[uuidBytes.length + hrBytes.length];
        System.arraycopy(uuidBytes, 0, payload, 0, uuidBytes.length);
        System.arraycopy(hrBytes, 0, payload, uuidBytes.length, hrBytes.length);
        return payload;
    }
}