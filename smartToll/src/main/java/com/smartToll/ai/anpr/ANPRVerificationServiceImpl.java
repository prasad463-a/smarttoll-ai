package com.smartToll.ai.anpr;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.smartToll.entity.TollPayment;
import com.smartToll.entity.Vehicle;
import com.smartToll.repository.TollPaymentRepository;
import com.smartToll.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ANPRVerificationServiceImpl
        implements ANPRVerificationService {

    private final VehicleRepository vehicleRepository;
    private final TollPaymentRepository tollPaymentRepository;

    @Override
    public Vehicle findVehicle(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            return null;
        }

        // 1. Direct exact match
        Vehicle v = vehicleRepository.findByvehicleNumber(vehicleNumber).orElse(null);
        if (v != null) {
            return v;
        }

        // 2. Cleaned normalized match (strip spaces, hyphens, uppercase)
        String cleanNumber = vehicleNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        Vehicle cleanedMatch = vehicleRepository.findByvehicleNumber(cleanNumber).orElse(null);
        if (cleanedMatch != null) {
            return cleanedMatch;
        }

        // 3. Fallback scan across registered vehicles ignoring punctuation and case
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        for (Vehicle vehicle : allVehicles) {
            if (vehicle.getVehicleNumber() != null) {
                String normalizedDbNumber = vehicle.getVehicleNumber().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                if (normalizedDbNumber.equals(cleanNumber)) {
                    return vehicle;
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasValidPayment(Vehicle vehicle) {
        if (vehicle == null) {
            return false;
        }

        List<TollPayment> payments =
                tollPaymentRepository
                        .findByVehicleAndPaymentStatusAndValidUntilAfter(
                                vehicle,
                                "SUCCESS",
                                LocalDateTime.now()
                        );

        if (payments.isEmpty()) {
            payments = tollPaymentRepository
                    .findByVehicleAndPaymentStatusAndValidUntilAfter(
                            vehicle,
                            "CLEARED",
                            LocalDateTime.now()
                    );
        }

        return !payments.isEmpty();
    }
}
