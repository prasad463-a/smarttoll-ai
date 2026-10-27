
package com.smartToll.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartToll.dto.LoginRequest;
import com.smartToll.dto.UserRequest;
import com.smartToll.dto.LoginResponse;
import com.smartToll.dto.UserResponse;
import com.smartToll.entity.User;
import com.smartToll.service.UserService;
import com.smartToll.util.EntityDtoConverter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")

public class UserController {

	  private final UserService userservice;

	    // Constructor: manually added
	    public UserController(UserService userservice) {
	        this.userservice = userservice;
	    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody UserRequest request) {

        UserResponse response = userservice.register(request);

        return new ResponseEntity<>(
            response,
            HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userservice.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @PathVariable Long id) {

        User user = userservice.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("User not found")
                );

        return ResponseEntity.ok(
            EntityDtoConverter.convertToResponse(user)
        );
    }

    @GetMapping("/getall")
    public ResponseEntity<List<UserResponse>> getAll() {

        List<UserResponse> users =
            userservice.getalluser()
                .stream()
                .map(EntityDtoConverter::convertToResponse)
                .toList();

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(
            @PathVariable Long id) {

        userservice.delete(id);

        return ResponseEntity.ok("User deleted successfully");
    }
}
    

