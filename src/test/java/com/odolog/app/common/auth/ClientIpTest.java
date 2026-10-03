package com.odolog.app.common.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpTest {

    private static MockHttpServletRequest from(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }

    @Test
    @DisplayName("IPv6 는 앞 64비트로 묶는다 — 한 가입자의 주소 범위 안에서 바꿔 가며 우회하지 못하게")
    void groupsIpv6ByPrefix() {
        assertThat(ClientIp.of(from("2001:db8:1:2:aaaa::1"))).isEqualTo(ClientIp.of(from("2001:db8:1:2:bbbb::9")));
        assertThat(ClientIp.of(from("2001:db8:1:3::1"))).isNotEqualTo(ClientIp.of(from("2001:db8:1:2::1")));
    }

    @Test
    @DisplayName("IPv4 는 그대로")
    void keepsIpv4() {
        assertThat(ClientIp.of(from("192.0.2.1"))).isEqualTo("192.0.2.1");
    }
}
