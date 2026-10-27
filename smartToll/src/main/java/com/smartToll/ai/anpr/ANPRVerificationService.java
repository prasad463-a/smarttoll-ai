package com.smartToll.ai.anpr;

import com.smartToll.entity.Vehicle;

public interface ANPRVerificationService {

    Vehicle findVehicle(String vehicleNumber);

    boolean hasValidPayment(Vehicle vehicle);
}