package com.smartToll.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.smartToll.dao.VehicleDao;
import com.smartToll.entity.Vehicle;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleDao vehicleDAO;

    @Override
    public Vehicle addVehicle(Vehicle vehicle) {
        return vehicleDAO.save(vehicle);
    }

    @Override
    public Vehicle getVehicleById(Long id) {

        return vehicleDAO.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));
    }

    @Override
    public Vehicle getVehicleByNumber(String vehicleNumber) {

        return vehicleDAO.findByVehicleNumber(vehicleNumber)
                .orElseThrow(() ->
                        new RuntimeException("Vehicle not found"));
    }

    @Override
    public List<Vehicle> getAllVehicles() {
        return vehicleDAO.findAll();
    }

    @Override
    public void deleteVehicle(Long id) {
        vehicleDAO.deleteById(id);
    }
}
