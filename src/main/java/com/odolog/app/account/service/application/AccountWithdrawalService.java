package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. user / vehicle / maintenance 를 모두 건드리는 유일한 동작
 * UserService 에 두면 user → vehicle 역방향, common 에 두면 순환이라 바깥에 조율 층을 둠
 * 하는 일은 순서 결정뿐 — 리포지토리를 직접 부르면 남의 테이블 구조를 알게 됨
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
        // 비밀번호 먼저. 롤백이 받아 주긴 하나 순서로 막을 수 있는 것을 롤백에 기대지 않음
        userService.verifyPassword(userId, request.password());

        // 이력 → 차량 → 사용자. 거꾸로면 FK 제약 위반
        vehicleService.deleteAllOwnedBy(userId);
        userService.delete(userId);
    }
}
