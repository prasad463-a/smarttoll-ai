
package com.smartToll.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartToll.dao.UserDao;
import com.smartToll.dto.LoginRequest;
import com.smartToll.dto.UserRequest;
import com.smartToll.dto.LoginResponse;
import com.smartToll.dto.UserResponse;
import com.smartToll.entity.User;
import com.smartToll.jwt.JwtUtil;
import com.smartToll.service.UserService;
import com.smartToll.util.EntityDtoConverter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userdao;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserResponse register(UserRequest request) {

        if (userdao.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = EntityDtoConverter.convertToEntity(request);

        user.setPassword(
            passwordEncoder.encode(request.getPassword())
        );

        User savedUser = userdao.save(user);

        return EntityDtoConverter.convertToResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userdao.login(request.getEmail())
                .orElseThrow(() ->
                    new RuntimeException("Invalid email or password")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setMessage("Login Successful");
        response.setToken(token);

        return response;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userdao.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userdao.findByEmail(email);
    }

    @Override
    public List<User> getalluser() {
        return userdao.findAll();
    }

    @Override
    public void delete(Long id) {
        userdao.deleteById(id);
    }
}