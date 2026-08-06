package com.chayewuu.hyperheartratebukkit.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * VarInt 编解码工具（与 Minecraft 网络协议兼容）。
 * <p>
 * 用于与 Fabric 模组的 {@code writeVarInt} / {@code readVarInt} 对齐，
 * 确保心率数据在 Bukkit 插件消息通道与 Fabric 模组之间正确编解码。
 * </p>
 *
 * <p>VarInt 编码规则：每字节高 1 位为继续标志（1=继续，0=末字节），
 * 低 7 位为数据位，小端序排列。</p>
 */
public final class VarIntUtil {

    private VarIntUtil() {
    }

    /**
     * 将 int 值编码为 VarInt 字节数组。
     *
     * @param value 要编码的 int 值
     * @return VarInt 编码后的字节数组（1~5 字节）
     */
    public static byte[] encodeVarInt(int value) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (true) {
            if ((value & ~0x7F) == 0) {
                bos.write(value);
                return bos.toByteArray();
            }
            bos.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * 从字节数组中读取 VarInt。
     *
     * @param data  字节数组
     * @param index 起始读取位置
     * @return 包含解析结果和读取字节数的 {@link VarIntResult}
     * @throws IllegalArgumentException 如果 VarInt 超过 5 字节或数据不足
     */
    public static VarIntResult decodeVarInt(byte[] data, int index) {
        int result = 0;
        int shift = 0;
        int bytesRead = 0;
        while (true) {
            if (index >= data.length) {
                throw new IllegalArgumentException("数据不足，无法完整读取 VarInt");
            }
            byte b = data[index];
            result |= (b & 0x7F) << shift;
            bytesRead++;
            index++;
            if ((b & 0x80) == 0) {
                break;
            }
            shift += 7;
            if (shift > 35) {
                throw new IllegalArgumentException("VarInt 过长（超过 5 字节）");
            }
        }
        return new VarIntResult(result, bytesRead);
    }

    /**
     * 将 UUID 编码为 16 字节数组（大端序，与 Minecraft 协议一致）。
     *
     * @param uuid UUID
     * @return 16 字节数组
     */
    public static byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >> (56 - i * 8));
        }
        for (int i = 0; i < 8; i++) {
            bytes[8 + i] = (byte) (lsb >> (56 - i * 8));
        }
        return bytes;
    }

    /**
     * 从 16 字节数组读取 UUID（大端序）。
     *
     * @param bytes 16 字节数组
     * @param index 起始读取位置
     * @return UUID
     */
    public static UUID uuidFromBytes(byte[] bytes, int index) {
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (bytes[index + i] & 0xFF);
        }
        for (int i = 0; i < 8; i++) {
            lsb = (lsb << 8) | (bytes[index + 8 + i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }

    /**
     * VarInt 解析结果。
     */
    public record VarIntResult(int value, int bytesRead) {
    }
}