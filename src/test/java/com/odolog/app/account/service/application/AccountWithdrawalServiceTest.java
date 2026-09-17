package com.odolog.app.account.service.application;

import com.odolog.app.account.dto.request.withdraw.WithdrawRequest;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.user.service.application.UserService;
import com.odolog.app.vehicle.service.application.VehicleService;
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
    private VehicleService vehicleService;

    @InjectMocks
    private AccountWithdrawalService accountWithdrawalService;

    @Test
    @DisplayName("비밀번호 확인 → 차량 삭제 → 사용자 삭제 순서로 진행한다")
    void withdrawFollowsOrder() {
        accountWithdrawalService.withdraw(1L, new WithdrawRequest("password1234"));

        InOrder order = inOrder(userService, vehicleService);
        order.verify(userService).verifyPassword(1L, "password1234");
        order.verify(vehicleService).deleteAllOwnedBy(1L);
        order.verify(userService).delete(1L);
    }

    @Test
    @DisplayName("비밀번호가 틀리면 차량도 사용자도 지우지 않는다")
    void withdrawWithWrongPasswordDeletesNothing() {
        doThrow(new AuthenticationFailedException("현재 비밀번호가 올바르지 않습니다."))
                .when(userService).verifyPassword(1L, "wrongpassword");

        assertThatThrownBy(() -> accountWithdrawalService.withdraw(1L, new WithdrawRequest("wrongpassword")))
                .isInstanceOf(AuthenticationFailedException.class);

        // 롤백이 아니라 순서 자체로 막았는지
        verify(vehicleService, never()).deleteAllOwnedBy(1L);
        verify(userService, never()).delete(1L);
    }
}
