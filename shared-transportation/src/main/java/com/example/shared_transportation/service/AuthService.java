package com.example.shared_transportation.service;

import com.example.shared_transportation.common.BusinessException;
import com.example.shared_transportation.config.JwtUtil;
import com.example.shared_transportation.dto.AuthRequest;
import com.example.shared_transportation.dto.AuthResponse;
import com.example.shared_transportation.dto.UserView;
import com.example.shared_transportation.entity.User;
import com.example.shared_transportation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(40001, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(40002, "手机号或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(40002, "手机号或密码错误");
        }
        if ("BANNED".equals(user.getStatus())) {
            throw new BusinessException(40300, "该账号已被封禁，请联系客服");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getPhone(), user.getRole());
        UserView view = new UserView(
                String.valueOf(user.getId()), user.getPhone(), user.getNickname(), user.getRole());
        return new AuthResponse(token, view);
    }
}
