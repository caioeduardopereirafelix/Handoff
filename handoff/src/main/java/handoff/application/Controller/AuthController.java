package handoff.application.Controller;

import handoff.application.Dto.LoginRequestDto;
import handoff.application.Dto.RefreshTokenRequestDto;
import handoff.application.Dto.RegisterRequestDto;
import handoff.application.Dto.TokenResponseDto;
import handoff.application.Service.AuthService;
import handoff.application.Service.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponseDto register(@RequestBody @Valid RegisterRequestDto registerRequestDto, HttpServletRequest http) {
        rateLimiter.checkRegister(http.getRemoteAddr());
        return authService.register(registerRequestDto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDto login(@RequestBody @Valid LoginRequestDto loginRequestDto, HttpServletRequest http) {
        rateLimiter.checkLogin(http.getRemoteAddr(), loginRequestDto.email());
        return authService.login(loginRequestDto);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDto refresh(@RequestBody @Valid RefreshTokenRequestDto request, HttpServletRequest http) {
        rateLimiter.checkRefresh(http.getRemoteAddr());
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody @Valid RefreshTokenRequestDto request) {
        authService.logout(request.refreshToken());
    }
}
