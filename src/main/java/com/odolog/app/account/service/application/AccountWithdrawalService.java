package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.service.application.VehicleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. user·vehicle·maintenance 에 걸친 조율 층
 * 순서만 결정, 실제 삭제는 각 기능 담당
 */
@Service
@Transactional(readOnly = true)
public class AccountWithdrawalService {

    private final UserService userService;
    private final VehicleService vehicleService;

    public AccountWithdrawalService(UserService userService, VehicleService vehicleService) {
        this.userService = userService;
        this.vehicleService = vehicleService;
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // 비밀번호 확인 먼저
        userService.verifyPassword(userId, request.password());

        // 이력 → 차량 → 사용자 순서. FK 제약
        vehicleService.deleteAllOwnedBy(userId);
        userService.delete(userId);
    }
}
