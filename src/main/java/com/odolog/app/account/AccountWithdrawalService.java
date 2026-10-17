package com.odolog.app.account;

import com.odolog.app.account.dto.request.WithdrawRequest;
import com.odolog.app.garage.VehicleRemovalService;
import com.odolog.app.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 회원 탈퇴. user·garage 에 걸친 조율 층
 * 순서만 결정, 실제 삭제는 각 기능 담당
 */
@Service
@Transactional(readOnly = true)
public class AccountWithdrawalService {

    private final UserService userService;
    private final VehicleRemovalService vehicleRemovalService;
    private final TransactionTemplate transactionTemplate;

    public AccountWithdrawalService(UserService userService, VehicleRemovalService vehicleRemovalService,
                                    PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.vehicleRemovalService = vehicleRemovalService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** BCrypt 는 트랜잭션 밖에서 먼저. 사용자 행 잠금과 커넥션을 쥔 채 돌지 않게 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void withdraw(Long userId, WithdrawRequest request) {
        String verifiedHash = userService.verifyPassword(userId, request.password());

        transactionTemplate.executeWithoutResult(status -> {
            // 첫 조회가 사용자 행 잠금. 같은 계정의 가져오기·재설정과 한 줄로 세움(엇갈리면 데드락)
            userService.lockIfPasswordUnchanged(userId, verifiedHash);

            // 이력 → 차량 → 사용자 순서. FK 제약
            vehicleRemovalService.deleteAllOwnedBy(userId);
            userService.delete(userId);
        });
    }
}
