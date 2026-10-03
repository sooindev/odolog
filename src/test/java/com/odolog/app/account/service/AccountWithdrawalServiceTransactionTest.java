package com.odolog.app.account.service;

import com.odolog.app.account.dto.request.WithdrawRequest;
import com.odolog.app.fuel.domain.FuelRecord;
import com.odolog.app.fuel.FuelRecordRepository;
import com.odolog.app.maintenance.domain.entity.MaintenanceRecord;
import com.odolog.app.maintenance.domain.entity.ServiceInterval;
import com.odolog.app.maintenance.domain.ServiceType;
import com.odolog.app.maintenance.repository.MaintenanceRecordRepository;
import com.odolog.app.maintenance.repository.ServiceIntervalRepository;
import com.odolog.app.user.domain.entity.PasswordResetToken;
import com.odolog.app.user.domain.entity.User;
import com.odolog.app.user.repository.PasswordResetTokenRepository;
import com.odolog.app.user.repository.UserRepository;
import com.odolog.app.vehicle.Vehicle;
import com.odolog.app.vehicle.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 탈퇴를 실제 DB 로. 자식 테이블이 전부 찬 계정
 * Mockito 의 InOrder 는 순서만 보고 FK 는 못 봄. 새 자식 테이블이 생기면 여기서 실패
 * 이 클래스에 @Transactional 금지. 서비스 설정이 무시됨
 */
@SpringBootTest
class AccountWithdrawalServiceTransactionTest {

    @Autowired
    private AccountWithdrawalService accountWithdrawalService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceRecordRepository maintenanceRecordRepository;

    @Autowired
    private ServiceIntervalRepository serviceIntervalRepository;

    @Autowired
    private FuelRecordRepository fuelRecordRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @AfterEach
    void tearDown() {
        // 실패했을 때의 잔여분. 자식 먼저
        passwordResetTokenRepository.deleteAll();
        fuelRecordRepository.deleteAll();
        serviceIntervalRepository.deleteAll();
        maintenanceRecordRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("기록·주기·재설정 토큰이 모두 있는 계정도 FK 에 막히지 않고 전부 지워진다")
    void withdrawsFullyPopulatedAccount() {
        User user = userRepository.save(new User("withdraw@odolog.com",
                new BCryptPasswordEncoder().encode("password1234"), "탈퇴자"));
        Vehicle vehicle = vehicleRepository.save(new Vehicle(user, "12가3456", "기아", "카니발", 2020));
        maintenanceRecordRepository.save(new MaintenanceRecord(vehicle, ServiceType.ENGINE_OIL,
                null, 80000, "KRW", 30000, LocalDate.of(2026, 5, 1)));
        serviceIntervalRepository.save(new ServiceInterval(vehicle, ServiceType.ENGINE_OIL, 10000, 12));
        fuelRecordRepository.save(new FuelRecord(vehicle, LocalDate.of(2026, 5, 2), 30100,
                new BigDecimal("50.00"), 90000, "KRW", null));
        passwordResetTokenRepository.save(new PasswordResetToken(user, "a".repeat(64),
                LocalDateTime.now().plusMinutes(30)));

        accountWithdrawalService.withdraw(user.getId(), new WithdrawRequest("password1234"));

        assertThat(userRepository.count()).isZero();
        assertThat(vehicleRepository.count()).isZero();
        assertThat(maintenanceRecordRepository.count()).isZero();
        assertThat(serviceIntervalRepository.count()).isZero();
        assertThat(fuelRecordRepository.count()).isZero();
        assertThat(passwordResetTokenRepository.count()).isZero();
    }
}
