package com.odolog.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기능 패키지 사이 의존 방향 고정. 소스의 import 를 훑음
 * 허용 목록에 없는 방향이 생기면 실패. 새 패키지는 여기에 먼저 등록
 */
class DependencyDirectionTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/com/odolog/app");

    /** 패키지 → import 해도 되는 패키지. 위가 아래를 알고, 아래는 위를 모름 */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "common", Set.of(),
            "user", Set.of("common"),
            "vehicle", Set.of("common", "user"),
            "maintenance", Set.of("common", "user", "vehicle"),
            "fuel", Set.of("common", "user", "vehicle"),
            // 조율 층. 여러 기능을 동시에 알아도 되는 자리
            "garage", Set.of("common", "user", "vehicle", "maintenance", "fuel"),
            "summary", Set.of("common", "user", "vehicle", "maintenance", "fuel"),
            "account", Set.of("common", "user", "vehicle", "maintenance", "fuel", "garage"));

    private static final Pattern IMPORT = Pattern.compile(
            "^import\\s+(?:static\\s+)?com\\.odolog\\.app\\.([a-z]+)\\.", Pattern.MULTILINE);

    private static final Pattern WILDCARD = Pattern.compile(
            "^import\\s+(?:static\\s+)?com\\.odolog\\.app\\..*\\*;", Pattern.MULTILINE);

    @Test
    @DisplayName("기능 패키지는 허용된 방향으로만 서로를 import 한다")
    void importsFollowAllowedDirection() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles()) {
            String owner = packageOf(file);
            if (owner == null) {
                continue;
            }
            String source = Files.readString(file);
            Matcher matcher = IMPORT.matcher(source);
            while (matcher.find()) {
                String target = matcher.group(1);
                if (!target.equals(owner) && !ALLOWED.get(owner).contains(target)) {
                    violations.add(SOURCE_ROOT.relativize(file) + " → " + target);
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("모든 기능 패키지가 허용 목록에 등록돼 있다")
    void everyPackageIsRegistered() throws IOException {
        try (Stream<Path> children = Files.list(SOURCE_ROOT)) {
            List<String> packages = children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .toList();

            assertThat(ALLOWED.keySet()).containsExactlyInAnyOrderElementsOf(packages);
        }
    }

    @Test
    @DisplayName("우리 패키지를 * 로 import 하지 않는다 — 이 검사가 방향을 읽지 못한다")
    void noWildcardImports() throws IOException {
        List<String> found = new ArrayList<>();
        for (Path file : javaFiles()) {
            if (WILDCARD.matcher(Files.readString(file)).find()) {
                found.add(SOURCE_ROOT.relativize(file).toString());
            }
        }

        assertThat(found).isEmpty();
    }

    private List<Path> javaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    /** 최상위 기능 패키지 이름. 루트의 OdoLogApplication 은 null */
    private String packageOf(Path file) {
        Path relative = SOURCE_ROOT.relativize(file);
        return relative.getNameCount() > 1 ? relative.getName(0).toString() : null;
    }
}
