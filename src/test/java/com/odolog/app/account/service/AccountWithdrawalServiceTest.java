package com.odolog.app.account.service;

import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.account.dto.request.WithdrawRequest;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.garage.service.VehicleRemovalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private VehicleRemovalService vehicleRemovalService;

    @InjectMocks
    private AccountWithdrawalService accountWithdrawalService;

    @Test
    @DisplayName("사용자 잠금 → 비밀번호 확인 → 차량 삭제 → 사용자 삭제 순서로 진행한다")
    void withdrawFollowsOrder() {
        accountWithdrawalService.withdraw(1L, new WithdrawRequest("password1234"));

        InOrder order = inOrder(userService, vehicleRemovalService);
        order.verify(userService).findByIdForUpdate(1L);
        order.verify(userService).verifyPassword(1L, "password1234");
        order.verify(vehicleRemovalService).deleteAllOwnedBy(1L);
        order.verify(userService).delete(1L);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 차량도 사용자도 지우지 않는다")
    void withdrawWithWrongPasswordDeletesNothing() {
        doThrow(new AuthenticationFailedException(ErrorCode.WRONG_PASSWORD, "현재 비밀번호가 올바르지 않습니다."))
                .when(userService).verifyPassword(1L, "wrongpassword");

        assertThatThrownBy(() -> accountWithdrawalService.withdraw(1L, new WithdrawRequest("wrongpassword")))
                .isInstanceOf(AuthenticationFailedException.class);

        // 롤백이 아닌 순서로 차단
        verify(vehicleRemovalService, never()).deleteAllOwnedBy(1L);
        verify(userService, never()).delete(1L);
    }
}
