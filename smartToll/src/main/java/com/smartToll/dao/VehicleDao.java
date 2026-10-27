package com.smartToll.dao;

import java.util.List;
import java.util.Optional;

import com.smartToll.entity.Vehicle;

public interface VehicleDao {

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(Long id);

    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);

    List<Vehicle> findAll();

    void deleteById(Long id);
}