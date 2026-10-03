package com.odolog.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLAUDE.md 의 구조 트리와 실제 파일을 대조
 * 문서를 손으로 대조하던 점검마다 트리가 낡아 있었다(빠진 파일·없는 파일). 그 일을 테스트로 넘긴다
 * 파일을 더하거나 옮기거나 지우면 트리도 같이 고쳐야 통과한다
 */
class DocumentationTreeTest {

    /** 트리에 있어야 하는 파일들 */
    private static final List<Path> SOURCE_ROOTS = List.of(
            Path.of("src/main/java"),
            Path.of("src/test/java"),
            Path.of("src/main/resources/db/migration"),
            Path.of("frontend/src"),
            Path.of("frontend/e2e"));

    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_.-]+\\.(?:java|tsx|ts|sql)\\b");

    @Test
    @DisplayName("모든 소스 파일이 CLAUDE.md 구조 트리에 있다")
    void everySourceFileIsListed() throws IOException {
        Set<String> listed = namesInTrees();

        Set<String> missing = new TreeSet<>(sourceFileNames());
        missing.removeAll(listed);

        assertThat(missing).as("CLAUDE.md 트리에 없는 파일").isEmpty();
    }

    @Test
    @DisplayName("CLAUDE.md 구조 트리에 적힌 파일이 실제로 있다")
    void everyListedFileExists() throws IOException {
        Set<String> phantom = new TreeSet<>(namesInTrees());
        phantom.removeAll(sourceFileNames());

        assertThat(phantom).as("트리에는 있지만 실제로는 없는 파일").isEmpty();
    }

    /** 트리 줄(├── · └──)에 적힌 파일 이름 */
    private Set<String> namesInTrees() throws IOException {
        Set<String> names = new TreeSet<>();
        for (String line : Files.readAllLines(Path.of("CLAUDE.md"))) {
            if (!line.contains("├── ") && !line.contains("└── ")) {
                continue;
            }
            String entry = line.substring(Math.max(line.indexOf("├── "), line.indexOf("└── ")) + 4);
            // 이름 칸만. 설명 칸의 파일 이름(다른 파일 언급)은 세지 않음
            String nameColumn = entry.split("\\s{2,}")[0];
            Matcher matcher = FILE_NAME.matcher(nameColumn);
            while (matcher.find()) {
                names.add(matcher.group());
            }
        }
        return names;
    }

    private Set<String> sourceFileNames() throws IOException {
        Set<String> names = new TreeSet<>();
        // 프론트 설정 파일(vite.config.ts 등)은 frontend/ 바로 아래만
        try (Stream<Path> files = Files.list(Path.of("frontend"))) {
            names.addAll(files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> FILE_NAME.matcher(name).matches())
                    .collect(Collectors.toSet()));
        }
        for (Path root : SOURCE_ROOTS) {
            try (Stream<Path> files = Files.walk(root)) {
                names.addAll(files.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .filter(name -> FILE_NAME.matcher(name).matches())
                        .collect(Collectors.toSet()));
            }
        }
        return names;
    }
}
