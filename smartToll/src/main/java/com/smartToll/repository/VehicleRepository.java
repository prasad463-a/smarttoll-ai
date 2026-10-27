package com.smartToll.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartToll.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
	Optional<Vehicle> findByvehicleNumber(String vehicleNumber);

}
