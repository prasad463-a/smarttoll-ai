package com.smartToll.dto;

import lombok.Data;

@Data
public class TollLoginRequest {

    private String email;

    private String password;
}