package com.odolog.app.account;

import com.odolog.app.account.dto.request.WithdrawRequest;
import com.odolog.app.garage.VehicleRemovalService;
import com.odolog.app.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. user·garage 에 걸친 조율 층
 * 순서만 결정, 실제 삭제는 각 기능 담당
 */
@Service
@Transactional(readOnly = true)
public class AccountWithdrawalService {

    private final UserService userService;
    private final VehicleRemovalService vehicleRemovalService;

    public AccountWithdrawalService(UserService userService, VehicleRemovalService vehicleRemovalService) {
        this.userService = userService;
        this.vehicleRemovalService = vehicleRemovalService;
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        // 첫 조회가 사용자 행 잠금. 같은 계정의 가져오기·재설정과 한 줄로 세움(엇갈리면 데드락)
        userService.findByIdForUpdate(userId);
        // 비밀번호 확인 먼저
        userService.verifyPassword(userId, request.password());

        // 이력 → 차량 → 사용자 순서. FK 제약
        vehicleRemovalService.deleteAllOwnedBy(userId);
        userService.delete(userId);
    }
}
