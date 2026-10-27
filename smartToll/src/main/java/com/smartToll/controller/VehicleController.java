package com.smartToll.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartToll.entity.Vehicle;
import com.smartToll.service.VehicleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/add")
    public Vehicle addVehicle(@RequestBody Vehicle vehicle) {

        return vehicleService.addVehicle(vehicle);
    }

    @GetMapping("/{id}")
    public Vehicle getVehicleById(@PathVariable Long id) {

        return vehicleService.getVehicleById(id);
    }

    @GetMapping("/number/{vehicleNumber}")
    public Vehicle getVehicleByNumber(
            @PathVariable String vehicleNumber) {

        return vehicleService.getVehicleByNumber(vehicleNumber);
    }

    @GetMapping("/getall")
    public List<Vehicle> getAllVehicles(org.springframework.security.core.Authentication authentication) {
        List<Vehicle> allVehicles = vehicleService.getAllVehicles();
        
        if (authentication != null && authentication.getPrincipal() instanceof com.smartToll.entity.User) {
            com.smartToll.entity.User user = (com.smartToll.entity.User) authentication.getPrincipal();
            
            // Admin can view all vehicles
            if ("ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole())) {
                return allVehicles;
            } 
            
            // Normal user can only view their own vehicles
            return allVehicles.stream()
                .filter(v -> v.getUser() != null && v.getUser().getId().equals(user.getId()))
                .toList();
        }
        
        // Default fallback (e.g., if Toll operator requests it)
        return allVehicles;
    }

    @DeleteMapping("/{id}")
    public String deleteVehicle(@PathVariable Long id) {

        vehicleService.deleteVehicle(id);

        return "Vehicle deleted successfully";
    }
}