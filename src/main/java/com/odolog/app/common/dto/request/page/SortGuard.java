package com.odolog.app.common.dto.request.page;

import com.odolog.app.common.exception.type.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 정렬할 수 있는 속성을 화이트리스트로 제한한다
 *
 * 왜 필요한가: Spring Data 는 `?sort=owner.password` 처럼 연관 엔티티를 타고 들어가는 정렬을
 * 그대로 받아 준다. 값이 응답에 실리지는 않지만 **암묵적 조인이 생기고, 의도한 적 없는 표면이
 * 열린다.** 없는 속성만 400 이 되던 상태(PropertyReferenceException)로는 이게 안 걸린다
 *
 * 블랙리스트가 아니라 화이트리스트인 이유: 엔티티에 필드를 하나 더하면 그 필드가 자동으로
 * 정렬 대상이 된다. 막을 것을 세는 쪽은 언제나 뒤처진다
 */
public final class SortGuard {

    private SortGuard() {
    }

    /** 허용 목록에 없는 속성으로 정렬하려 하면 400 */
    public static void allowOnly(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new InvalidRequestException(
                        "sort: 정렬할 수 없는 속성입니다 (" + order.getProperty() + ")");
            }
        }
    }
}
