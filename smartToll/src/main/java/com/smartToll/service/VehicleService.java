package com.smartToll.service;

import java.util.List;
import java.util.Optional;

import com.smartToll.entity.Vehicle;

public interface VehicleService {
	
	 Vehicle addVehicle(Vehicle vehicle);

	    Vehicle getVehicleById(Long id);

	    Vehicle getVehicleByNumber(String vehicleNumber);

	    List<Vehicle> getAllVehicles();

	    void deleteVehicle(Long id);
}
