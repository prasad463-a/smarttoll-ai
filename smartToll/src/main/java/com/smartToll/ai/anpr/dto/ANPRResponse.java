package com.smartToll.ai.anpr.dto;

import lombok.Data;

@Data
public class ANPRResponse {

    private String vehicleNumber;

    private boolean vehicleDetected;

    private String message;
    
    private boolean paymentValid;
    
    private String gateDecision;
}
