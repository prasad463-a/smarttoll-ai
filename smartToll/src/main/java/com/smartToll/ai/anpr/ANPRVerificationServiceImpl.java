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

        return vehicleRepository
                .findByvehicleNumber(vehicleNumber)
                .orElse(null);
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

    return !payments.isEmpty();
}
}