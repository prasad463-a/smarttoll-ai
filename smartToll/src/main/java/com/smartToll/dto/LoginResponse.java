
package com.smartToll.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private Long id;

    private String name;

    private String email;

    private String role;

    private String message;

    private String token;
}