package com.odolog.app.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 백엔드 ErrorCode 와 화면의 ERROR_CODES 가 같은지. 한쪽에만 더하면 화면이 그 코드를 모르고 대신 쓰는 문구를 띄움
 * 화면 쪽 사전 누락은 errorMessage.test.ts 가 봄
 */
class ErrorCodeSyncTest {

    private static final Path FRONTEND_TYPES = Path.of("frontend/src/shared/api/types.ts");

    @Test
    @DisplayName("백엔드 ErrorCode 와 프론트 ERROR_CODES 가 같은 목록이다")
    void backendAndFrontendCodesMatch() throws Exception {
        String source = Files.readString(FRONTEND_TYPES);
        Matcher block = Pattern.compile("ERROR_CODES = \\[(.*?)] as const", Pattern.DOTALL).matcher(source);
        assertThat(block.find()).as("types.ts 에서 ERROR_CODES 를 찾지 못함").isTrue();

        Set<String> frontend = new LinkedHashSet<>();
        Matcher code = Pattern.compile("'([A-Z_]+)'").matcher(block.group(1));
        while (code.find()) {
            frontend.add(code.group(1));
        }
        Set<String> backend = Arrays.stream(ErrorCode.values()).map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(frontend).containsExactlyInAnyOrderElementsOf(backend);
    }
}
