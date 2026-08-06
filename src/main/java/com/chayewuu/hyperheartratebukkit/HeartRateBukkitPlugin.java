package com.chayewuu.hyperheartratebukkit;

import com.chayewuu.hyperheartratebukkit.network.HeartRateMessageListener;
import com.chayewuu.hyperheartratebukkit.network.RemoteHeartRateStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hyper-Heartrate Bukkit 插件主类。
 * <p>
 * 作为 Minecraft 服务端中转插件，接收装有 Fabric 模组的客户端发送的心率数据，
 * 并广播给附近安装了模组的其他玩家。
 * </p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 *   <li>注册插件消息通道（C2S 接收、S2C 发送）</li>
 *   <li>接收客户端心率数据，存入 {@link RemoteHeartRateStore}</li>
 *   <li>广播给 64 格内安装了模组的其他玩家</li>
 *   <li>玩家退出时清理其心率数据</li>
 * </ul>
 *
 * <p>与 Fabric 模组 {@code hyper-heartrate} 配合使用。
 * Fabric 模组负责客户端的心率采集与显示，本插件负责服务端的中转。</p>
 */
public class HeartRateBukkitPlugin extends JavaPlugin implements Listener {

    private HeartRateMessageListener messageListener;
    private RemoteHeartRateStore store;
    private HeartRatePlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        getLogger().info("============================================");
        getLogger().info("  HyperHeart-Bukkit 正在加载...");
        getLogger().info("============================================");

        // 初始化存储
        this.store = RemoteHeartRateStore.getInstance();

        // 初始化消息监听器
        this.messageListener = new HeartRateMessageListener(this);

        // 注册插件消息通道
        // C2S: 接收客户端发来的心率数据
        Bukkit.getMessenger().registerIncomingPluginChannel(this,
                HeartRateMessageListener.CHANNEL_C2S, messageListener);
        // S2C: 向客户端发送其他玩家的心率数据
        Bukkit.getMessenger().registerOutgoingPluginChannel(this,
                HeartRateMessageListener.CHANNEL_S2C);

        // 注册事件监听器（玩家退出清理）
        Bukkit.getPluginManager().registerEvents(this, this);

        // 注册 PlaceholderAPI 变量扩展
        registerPlaceholderExpansion();

        getLogger().info("C2S 通道已注册: " + HeartRateMessageListener.CHANNEL_C2S);
        getLogger().info("S2C 通道已注册: " + HeartRateMessageListener.CHANNEL_S2C);
        getLogger().info("HyperHeart-Bukkit 已启用，版本: " + getPluginMeta().getVersion());
    }

    /**
     * 注册 PlaceholderAPI 变量扩展。
     * <p>如果服务端未安装 PlaceholderAPI，跳过注册并记录日志。</p>
     */
    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI 未安装，跳过变量扩展注册");
            return;
        }
        this.placeholderExpansion = new HeartRatePlaceholderExpansion(this);
        if (placeholderExpansion.register()) {
            getLogger().info("PlaceholderAPI 变量扩展已注册: %hyperheartrate_heartrate%");
        } else {
            getLogger().warning("PlaceholderAPI 变量扩展注册失败");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("正在卸载 HyperHeart-Bukkit...");

        // 取消注册插件消息通道
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);

        // 注销 PlaceholderAPI 变量扩展
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }

        // 清理所有心率数据
        if (store != null) {
            store.clear();
        }

        getLogger().info("HyperHeart-Bukkit 已卸载");
    }

    /**
     * 玩家退出时清理其心率数据。
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (store != null) {
            store.removePlayer(event.getPlayer().getUniqueId());
        }
    }
}