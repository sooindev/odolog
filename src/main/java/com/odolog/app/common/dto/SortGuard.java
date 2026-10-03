package com.odolog.app.common.dto;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
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

    /**
     * 허용 목록 밖 정렬은 400. 통과하면 id 를 마지막 정렬 기준으로 덧붙여 돌려줌
     * 동점(같은 비용·같은 등록 시각)끼리는 DB 가 순서를 보장하지 않아 페이지마다 겹치거나 빠짐
     */
    public static Pageable allowOnly(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException(ErrorCode.INVALID_SORT,
                        "sort: 정렬할 수 없는 속성입니다 (" + order.getProperty() + ")", "sort");
            }
        }

        Sort sort = pageable.getSort();
        if (sort.isUnsorted() || sort.getOrderFor("id") != null) {
            return pageable;
        }

        // 마지막 기준과 같은 방향. 최신순 목록이면 동점도 나중 것이 위
        Sort.Direction direction = sort.stream().reduce((first, second) -> second).orElseThrow().getDirection();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort.and(Sort.by(direction, "id")));
    }
}
