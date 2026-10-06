package com.peakui.gateway.config;

import io.netty.util.NetUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/** 使用 Gateway 已依赖的 Netty 解析 IP 字面量，绝不执行 DNS 查询。 */
final class IpAddressRules {
    private IpAddressRules() {
    }

    static byte[] address(String value) {
        if (value == null || value.isBlank() || value.contains("%") || value.contains("[")
                || value.contains("]") || (!NetUtil.isValidIpV4Address(value) && !NetUtil.isValidIpV6Address(value))) {
            throw new IllegalArgumentException("IP 必须是 IPv4 或 IPv6 字面量");
        }
        try {
            // InetAddress.getByAddress 不查询 DNS，并统一 IPv4-mapped IPv6 与 IPv4。
            return InetAddress.getByAddress(NetUtil.createByteArrayFromIpAddressString(value)).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无效 IP", e);
        }
    }

    static String canonical(String value) {
        try {
            return InetAddress.getByAddress(address(value)).getHostAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("无效 IP", e);
        }
    }

    static List<Network> parse(String rules) {
        if (rules == null || rules.isBlank()) {
            return List.of();
        }
        // 非空但无效的配置拒绝请求，不能静默变成空白名单。
        return Arrays.stream(rules.split(",", -1)).map(String::trim).map(Network::parse).toList();
    }

    static boolean matches(List<Network> rules, String ip) {
        byte[] candidate = address(ip);
        return rules.stream().anyMatch(rule -> rule.matches(candidate));
    }

    record Network(byte[] address, int prefix) {
        static Network parse(String rule) {
            String[] parts = rule.split("/", -1);
            if (parts.length > 2) {
                throw new IllegalArgumentException("无效 CIDR");
            }
            byte[] bytes = IpAddressRules.address(parts[0]);
            int prefix = parts.length == 1 ? bytes.length * 8 : Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > bytes.length * 8) {
                throw new IllegalArgumentException("无效 CIDR 前缀");
            }
            return new Network(bytes, prefix);
        }

        boolean matches(byte[] candidate) {
            if (candidate.length != address.length) {
                return false;
            }
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % 8);
                if ((candidate[bit / 8] & mask) != (address[bit / 8] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
