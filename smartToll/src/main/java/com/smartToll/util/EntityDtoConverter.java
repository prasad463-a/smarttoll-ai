
package com.smartToll.util;

import com.smartToll.dto.UserRequest;
import com.smartToll.dto.UserResponse;
import com.smartToll.entity.User;

public class EntityDtoConverter {

    public static User convertToEntity(UserRequest request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhone(request.getPhone());

        // Every public registration gets USER role
        user.setRole("USER");

        return user;
    }

    public static UserResponse convertToResponse(User user) {

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        return response;
    }
}