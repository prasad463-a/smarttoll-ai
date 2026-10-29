package com.smartToll.ai.anpr.dto;

import lombok.Data;

@Data
public class ANPRRequest {

    private String imagePath;
    private String imageBase64;
}
