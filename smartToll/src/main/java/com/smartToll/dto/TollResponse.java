package com.smartToll.dto;

import lombok.Data;

@Data
public class TollResponse {

    private Long id;

    private String name;

    private String email;

    private String location;

    private Double amount;
 // JWT token
    private String token;
}