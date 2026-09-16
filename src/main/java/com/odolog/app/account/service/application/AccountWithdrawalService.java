package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. user / vehicle / maintenance 를 모두 건드리는 유일한 동작이다.
 *
 * <p><b>왜 account 라는 새 패키지인가.</b> 의존 방향이 {@code maintenance → vehicle → user}
 * 라서, 이 코드를 UserService 에 넣으면 {@code user → vehicle} 역방향 의존이 생긴다.
 * user 는 아무것도 모르는 가장 안쪽 층이고, 거기에 차량 지식이 들어가면 그 층의 의미가 사라진다.
 * common 도 답이 아니다 — 세 기능이 전부 common 을 의존하므로 common 이 vehicle 을 알면 순환이다.
 *
 * <p>그래서 세 기능보다 <b>바깥</b>에 조율만 하는 자리를 새로 뒀다. 프론트엔드의 {@code app/}
 * 층("여러 기능을 동시에 알아도 되는 유일한 층")과 같은 성격이고, 같은 이유로 존재한다.
 *
 * <p>이 클래스가 하는 일은 순서를 정하는 것뿐이다. 실제 삭제는 각 기능이 자기 것을 지운다 —
 * 여기서 리포지토리를 직접 부르면 조율 층이 남의 테이블 구조를 알게 된다.
 */
@Service
public class AccountWithdrawalService {

    private final UserService userService;
    private final VehicleService vehicleService;

    public AccountWithdrawalService(UserService userService, VehicleService vehicleService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // 비밀번호를 가장 먼저 확인한다. 뒤로 미루면 틀린 비밀번호로도 차량이 먼저 지워진다.
        // (트랜잭션이 롤백해 주긴 하지만, 순서로 막을 수 있는 것을 롤백에 기대지 않는다.)
        userService.verifyPassword(userId, request.password());

        // 이력 → 차량 → 사용자. 거꾸로 하면 FK 제약에 걸린다.
        vehicleService.deleteAllOwnedBy(userId);
        userService.delete(userId);
    }
}
