package com.odolog.app.common.dto.request.page;

import com.odolog.app.common.exception.type.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 정렬 가능 속성 화이트리스트
 * ?sort=owner.password 같은 연관 엔티티 정렬 차단
 * 블랙리스트 미사용. 새 필드가 자동으로 열리는 문제
 */
public final class SortGuard {

    private SortGuard() {
    }

    /** 허용 목록 밖 정렬은 400 */
    public static void allowOnly(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException(
                        "sort: 정렬할 수 없는 속성입니다 (" + order.getProperty() + ")");
            }
        }
    }
}
