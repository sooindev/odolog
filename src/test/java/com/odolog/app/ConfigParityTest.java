package com.odolog.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 테스트 설정(src/test/resources/application.yml)은 운영 설정을 통째로 가린다 — 테스트가 운영 DB 를
 * 지울 수 없게 일부러 그렇게 둔다. 대가로 세션·페이지 같은 값은 양쪽에 따로 적혀 있어, 한쪽만 고치면
 * 테스트와 E2E 가 운영과 다른 설정으로 돈다. 그 값들이 같은지 여기서 본다
 */
class ConfigParityTest {

    /** 양쪽이 같아야 하는 키. 환경마다 다른 값(DB·메일·secure·CORS)은 넣지 않는다 */
    private static final List<String> SHARED_KEYS = List.of(
            "server.servlet.session.timeout",
            "server.servlet.session.cookie.name",
            "server.servlet.session.cookie.http-only",
            "server.servlet.session.cookie.same-site",
            "server.servlet.session.cookie.max-age",
            "spring.data.web.pageable.max-page-size",
            "spring.jpa.open-in-view",
            "spring.session.jdbc.initialize-schema",
            "spring.session.jdbc.flush-mode");

    @Test
    @DisplayName("운영과 테스트 설정이 세션·쿠키·페이지 상한에서 같다")
    void sharedKeysMatch() throws IOException {
        Map<String, Object> main = load(Path.of("src/main/resources/application.yml"));
        Map<String, Object> test = load(Path.of("src/test/resources/application.yml"));

        for (String key : SHARED_KEYS) {
            assertThat(String.valueOf(lookup(test, key))).as(key).isEqualTo(String.valueOf(lookup(main, key)));
            assertThat(lookup(main, key)).as(key + " 가 운영 설정에 없음").isNotNull();
        }
    }

    @Test
    @DisplayName("프로파일 없이 떠도 운영에 안전하다 — 개발 편의(SQL·바인딩 로그, 문서, 재설정 링크 로그)는 dev 에만")
    void baseConfigIsSafeWithoutProfile() throws IOException {
        Map<String, Object> main = load(Path.of("src/main/resources/application.yml"));

        assertThat(lookup(main, "logging.level.org.hibernate.orm.jdbc.bind")).isNotEqualTo("trace");
        assertThat(lookup(main, "logging.level.org.hibernate.SQL")).isNotEqualTo("debug");
        assertThat(lookup(main, "spring.jpa.show-sql")).isNotEqualTo(true);
        assertThat(lookup(main, "springdoc.api-docs.enabled")).isEqualTo(false);
        assertThat(lookup(main, "springdoc.swagger-ui.enabled")).isEqualTo(false);
        assertThat(lookup(main, "odolog.mail.log-link-on-failure")).isEqualTo(false);
        assertThat(lookup(main, "server.servlet.session.cookie.secure")).isEqualTo("${SESSION_COOKIE_SECURE:true}");
    }

    private Map<String, Object> load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private Object lookup(Map<String, Object> root, String dottedKey) {
        Object current = root;
        for (String part : dottedKey.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }
}
