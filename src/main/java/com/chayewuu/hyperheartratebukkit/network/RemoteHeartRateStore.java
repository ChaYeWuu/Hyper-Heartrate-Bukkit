package com.chayewuu.hyperheartratebukkit.network;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程玩家心率存储（单例）。
 * <p>
 * 存储从客户端接收的心率数据，供其他插件查询。
 * 数据带有过期机制（10 秒无更新自动清除），避免玩家断开后残留。
 * 与 Fabric 模组的 {@code RemoteHeartRateStore} 行为一致。
 * </p>
 */
public class RemoteHeartRateStore {
    private static volatile RemoteHeartRateStore instance;

    private final Map<UUID, Integer> heartRates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastUpdate = new ConcurrentHashMap<>();

    private RemoteHeartRateStore() {
    }

    public static RemoteHeartRateStore getInstance() {
        if (instance == null) {
            synchronized (RemoteHeartRateStore.class) {
                if (instance == null) {
                    instance = new RemoteHeartRateStore();
                }
            }
        }
        return instance;
    }

    /**
     * 更新远程玩家心率。
     *
     * @param playerUuid 玩家 UUID
     * @param heartRate  心率值（0 表示无心率/断开）
     */
    public void updateHeartRate(UUID playerUuid, int heartRate) {
        if (heartRate <= 0) {
            heartRates.remove(playerUuid);
            lastUpdate.remove(playerUuid);
        } else {
            heartRates.put(playerUuid, heartRate);
            lastUpdate.put(playerUuid, System.currentTimeMillis());
        }
    }

    /**
     * 获取远程玩家心率。
     *
     * @param playerUuid 玩家 UUID
     * @return 心率值，0 表示无数据或已过期
     */
    public int getHeartRate(UUID playerUuid) {
        Long last = lastUpdate.get(playerUuid);
        if (last == null) {
            return 0;
        }
        if (System.currentTimeMillis() - last > 10000) {
            heartRates.remove(playerUuid);
            lastUpdate.remove(playerUuid);
            return 0;
        }
        Integer hr = heartRates.get(playerUuid);
        return hr != null ? hr : 0;
    }

    /**
     * 移除玩家数据（玩家退出时调用）。
     *
     * @param playerUuid 玩家 UUID
     */
    public void removePlayer(UUID playerUuid) {
        heartRates.remove(playerUuid);
        lastUpdate.remove(playerUuid);
    }

    /**
     * 清除所有数据。
     */
    public void clear() {
        heartRates.clear();
        lastUpdate.clear();
    }

    /**
     * 获取所有正在跟踪的玩家 UUID 集合。
     *
     * @return 玩家 UUID 集合
     */
    public Set<UUID> getTrackedPlayers() {
        return heartRates.keySet();
    }
}