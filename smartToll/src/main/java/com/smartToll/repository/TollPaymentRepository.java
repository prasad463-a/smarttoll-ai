package com.smartToll.repository;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.data.jpa.repository.JpaRepository;

import com.smartToll.entity.TollPayment;
import com.smartToll.entity.Vehicle;

public interface TollPaymentRepository extends JpaRepository<TollPayment, Long> {

	List<TollPayment> findByVehicleAndPaymentStatusAndValidUntilAfter(
            Vehicle vehicle,
            String paymentStatus,
            LocalDateTime currentTime
    );
}