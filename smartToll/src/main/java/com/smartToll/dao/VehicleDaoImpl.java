package com.smartToll.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.smartToll.entity.Vehicle;
import com.smartToll.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VehicleDaoImpl implements VehicleDao {

    private final VehicleRepository vehicleRepository;

    @Override
    public Vehicle save(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Override
    public Optional<Vehicle> findByVehicleNumber(String vehicleNumber) {
        return vehicleRepository.findByvehicleNumber(vehicleNumber);
    }

    @Override
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        vehicleRepository.deleteById(id);
    }
}