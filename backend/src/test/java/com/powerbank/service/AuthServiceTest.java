package com.powerbank.service;

import com.powerbank.dto.AuthResponse;
import com.powerbank.dto.LoginRequest;
import com.powerbank.entity.AccountStatus;
import com.powerbank.entity.UserAccount;
import com.powerbank.repository.AuthTokenRepository;
import com.powerbank.repository.UserAccountRepository;
import com.powerbank.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AuthServiceTest {
    @Test
    void loginAcceptsRegisteredPhoneNumber() {
        UserAccount user = new UserAccount("Test Customer", "test@example.com", "+201001234567", "hash");
        user.setAccountStatus(AccountStatus.ACTIVE);
        UserAccountRepository users = (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(), new Class<?>[]{UserAccountRepository.class},
                (proxy, method, args) -> method.getName().equals("findByPhone")
                        ? Optional.of(user) : Optional.empty());
        PasswordEncoder encoder = new PasswordEncoder() {
            @Override public String encode(CharSequence rawPassword) { return "hash"; }
            @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return rawPassword.toString().equals("StrongPass123!") && encodedPassword.equals("hash");
            }
        };
        AuthTokenRepository tokens = (AuthTokenRepository) Proxy.newProxyInstance(
                AuthTokenRepository.class.getClassLoader(), new Class<?>[]{AuthTokenRepository.class},
                (proxy, method, args) -> Optional.empty());
        AuthService service = new AuthService(users, tokens, mock(AuthCodeDeliveryService.class), encoder,
                new JwtService("0123456789abcdef0123456789abcdef", 60));

        AuthResponse result = service.login(new LoginRequest("+201001234567", "StrongPass123!"));

        assertNotNull(result);
        assertNotNull(result.token());
    }
}
