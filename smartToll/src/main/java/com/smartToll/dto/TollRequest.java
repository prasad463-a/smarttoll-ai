package com.smartToll.dto;

import lombok.Data;

@Data
public class TollRequest {

    private String name;

    private String email;

    private String password;

    private String location;

    private Double amount;
}