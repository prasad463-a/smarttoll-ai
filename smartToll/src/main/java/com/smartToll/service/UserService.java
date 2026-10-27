
package com.smartToll.service;

import java.util.List;
import java.util.Optional;

import com.smartToll.dto.LoginRequest;
import com.smartToll.dto.UserRequest;
import com.smartToll.dto.LoginResponse;
import com.smartToll.dto.UserResponse;
import com.smartToll.entity.User;

public interface UserService {

    UserResponse register(UserRequest request);

    LoginResponse login(LoginRequest request);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    List<User> getalluser();

    void delete(Long id);
}