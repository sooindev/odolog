package com.odolog.app.user;

import com.odolog.app.common.auth.LoginAttemptLimiter;
import com.odolog.app.common.exception.ErrorCode;
import com.odolog.app.common.exception.type.AuthenticationFailedException;
import com.odolog.app.common.exception.type.ConflictException;
import com.odolog.app.common.exception.type.InvalidRequestException;
import com.odolog.app.common.validation.InputText;
import com.odolog.app.user.domain.Language;
import com.odolog.app.user.domain.UnitSystem;
import com.odolog.app.user.domain.User;
import com.odolog.app.user.dto.request.ChangePasswordRequest;
import com.odolog.app.user.dto.request.LoginRequest;
import com.odolog.app.user.dto.request.SignUpRequest;
import com.odolog.app.user.dto.request.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    /** 로그인 시도 키. 접두사 없이 이메일만 쓰면 다른 용도의 키("password-check:1")와 겹침 */
    public static final String LOGIN_KEY_PREFIX = "login:";

    /** 로그인·가입·재설정과 공용 리미터, 키만 구분. 세션 사용자 id 기준 */
    public static final String PASSWORD_CHECK_KEY_PREFIX = "password-check:";

    /** 없는 계정의 비교 상대. 같은 인코더로 생성해 비용 동일 */
    private final String dummyHash;

    public UserService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       LoginAttemptLimiter loginAttemptLimiter,
                       PasswordEncoder passwordEncoder,
                       PlatformTransactionManager transactionManager) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.dummyHash = passwordEncoder.encode("no-such-account");
    }

    /** BCrypt 는 트랜잭션 밖. 저장은 리포지토리의 트랜잭션, 동시 가입은 이메일 유니크가 409 로 막음 */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User signUp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException(ErrorCode.EMAIL_DUPLICATE, "이미 가입된 이메일입니다: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.email(), encodedPassword, InputText.required(request.nickname(), "nickname"));
        applySettings(user, request.language(), request.timeZone(), request.currency(), request.unitSystem());

        return userRepository.save(user);
    }

    /** null 이 아닌 설정만 반영. 가입·프로필 수정 공용 */
    private void applySettings(User user, Language language, String timeZone,
                               String currency, UnitSystem unitSystem) {
        if (language != null) {
            user.changeLanguage(language);
        }
        if (timeZone != null) {
            user.changeTimeZone(timeZone);
        }
        if (currency != null) {
            user.changeCurrency(currency);
        }
        if (unitSystem != null) {
            user.changeUnitSystem(unitSystem);
        }
    }

    /**
     * 트랜잭션 밖. 조회만 짧게 하고 BCrypt(수십 ms)는 커넥션을 쥐지 않은 채
     * 클래스의 읽기 전용을 따르면 틀린 로그인이 몰릴 때 풀(10개)이 BCrypt 동안 묶여 다른 API 가 기다림
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public User login(LoginRequest request, String clientIp) {
        // 검증보다 먼저 집계. 잠긴 동안은 맞는 비밀번호도 거절
        // 이메일 + IP 짝으로 10번. 남의 주소로 일부러 틀려도 잠기는 건 그 IP 의 짝뿐
        // 계정 전체는 다섯 배. 여러 IP 로 흩은 대입 대비. IP 하나는 짝 한도(10)까지만 보탤 수 있어 다섯 곳 이상 필요
        String accountKey = LOGIN_KEY_PREFIX + request.email();
        String pairKey = LoginAttemptLimiter.pair(accountKey, clientIp);
        loginAttemptLimiter.acquirePaired(accountKey, clientIp, ErrorCode.TOO_MANY_LOGIN_ATTEMPTS, "로그인 시도가 너무 많습니다.");

        User user = userRepository.findByEmail(request.email()).orElse(null);

        // 없는 계정도 BCrypt 한 번. 응답 시간으로 가입 여부가 드러나는 것 방지
        String hash = (user == null) ? dummyHash : user.getPassword();
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (user == null || !matches) {
            throw new AuthenticationFailedException(ErrorCode.LOGIN_FAILED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptLimiter.recordSuccess(pairKey);
        loginAttemptLimiter.recordSuccess(accountKey);
        return user;
    }

    /** 로그인이 맞춰 본 해시가 지금도 그대로인지. 새 트랜잭션이라 그 사이 커밋된 변경·탈퇴가 보임 */
    public boolean isPasswordCurrent(Long userId, String matchedHash) {
        return userRepository.findById(userId)
                .map(user -> user.getPassword().equals(matchedHash))
                .orElse(false);
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));
    }

    /**
     * 사용자 행을 잠그고 조회. 같은 사용자의 다음 요청은 이 트랜잭션이 끝날 때까지 대기
     * 트랜잭션의 첫 조회여야 함. 그 전에 읽은 것이 있으면 대기 뒤에도 옛 스냅숏을 봄
     */
    @Transactional
    public User findByIdForUpdate(Long userId) {
        return userRepository.findLockedById(userId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 사용자입니다: " + userId));
    }

    /**
     * 되돌릴 수 없는 동작 앞의 비밀번호 확인. 변경·탈퇴 공용, BCrypt 라 트랜잭션 밖
     * 로그인과 같은 횟수 제한. 없으면 훔친 세션으로 현재 비밀번호 무제한 대입
     * 맞춰 본 해시를 돌려줌. 부르는 쪽이 lockIfPasswordUnchanged 로 잠근 뒤 그대로인지 다시 봄
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String verifyPassword(Long userId, String rawPassword) {
        String limitKey = PASSWORD_CHECK_KEY_PREFIX + userId;
        loginAttemptLimiter.acquire(limitKey, ErrorCode.TOO_MANY_PASSWORD_ATTEMPTS, "비밀번호를 너무 많이 틀렸습니다.");

        User user = findById(userId);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthenticationFailedException(ErrorCode.WRONG_PASSWORD, "현재 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptLimiter.recordSuccess(limitKey);
        return user.getPassword();
    }

    /** 첫 조회가 사용자 행 잠금. 확인한 뒤 재설정·다른 변경이 끝나 해시가 바뀌었으면 401 */
    @Transactional
    public User lockIfPasswordUnchanged(Long userId, String verifiedHash) {
        User user = findByIdForUpdate(userId);
        if (!user.getPassword().equals(verifiedHash)) {
            throw new AuthenticationFailedException(ErrorCode.WRONG_PASSWORD, "현재 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    @Transactional
    public void delete(Long userId) {
        // 재설정 토큰 먼저 삭제. FK 제약
        passwordResetTokenRepository.deleteByUserId(userId);
        userRepository.delete(findById(userId));
    }

    /**
     * BCrypt 셋(확인·같은지·새 해시)은 트랜잭션 밖에서 먼저. 잠금과 커넥션을 쥔 채 수백 ms 돌지 않게
     * 잠근 뒤에는 맞춰 본 해시가 그대로인지만 봄. 그 사이 재설정·다른 변경이 끝났으면 401
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        // 현재 비밀번호 확인. 열린 세션만으로는 변경 불가
        String verifiedHash = verifyPassword(userId, request.currentPassword());

        // 같은 비밀번호로 변경 거부. 바뀌지 않았는데 바뀐 것처럼 보이는 문제 방지
        // 재설정에는 미적용(옛 비밀번호를 모르는 사용자)
        if (passwordEncoder.matches(request.newPassword(), verifiedHash)) {
            throw new InvalidRequestException(ErrorCode.SAME_PASSWORD, "새 비밀번호가 현재 비밀번호와 같습니다.");
        }
        String newHash = passwordEncoder.encode(request.newPassword());

        transactionTemplate.executeWithoutResult(status -> {
            // 재설정 확정과 겹치면 늦은 쪽이 앞선 쪽의 결과를 보고 진행
            User user = lockIfPasswordUnchanged(userId, verifiedHash);

            user.changePassword(newHash);
            // 발급된 재설정 링크 폐기. 남겨 두면 바꾼 비밀번호를 30분 동안 링크로 덮어쓸 수 있음
            passwordResetTokenRepository.deleteByUserId(userId);
        });
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findById(userId);

        if (request.nickname() != null) {
            user.changeNickname(InputText.required(request.nickname(), "nickname"));
        }
        applySettings(user, request.language(), request.timeZone(), request.currency(), request.unitSystem());

        return user;
    }
}
