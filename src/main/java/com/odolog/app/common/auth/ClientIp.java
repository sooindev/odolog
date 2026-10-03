package com.odolog.app.common.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HexFormat;

/**
 * 시도 횟수를 IP 로 셀 때의 키. 가입·재설정 요청 공용
 * X-Forwarded-For 미사용. 신뢰할 프록시가 없어 헤더 조작으로 우회 가능
 * IPv6 는 앞 64비트로 묶음. 한 가입자가 받는 /64 안에서 주소만 바꿔 우회하는 것 방지
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        if (address == null || !address.contains(":")) {
            return address;
        }
        try {
            // 문자열 주소라 DNS 조회 없음
            byte[] bytes = InetAddress.getByName(address).getAddress();
            if (bytes.length != 16) {
                return address;
            }
            return HexFormat.of().formatHex(bytes, 0, 8) + "/64";
        } catch (UnknownHostException e) {
            return address;
        }
    }
}
