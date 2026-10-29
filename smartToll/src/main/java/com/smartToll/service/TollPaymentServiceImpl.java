package com.smartToll.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.smartToll.dao.TollPaymentDAO;
import com.smartToll.entity.TollPayment;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TollPaymentServiceImpl implements TollPaymentService {

    private final TollPaymentDAO tollPaymentDAO;

    @Override
    public TollPayment addPayment(TollPayment tollPayment) {
        if (tollPayment.getPaymentStatus() == null 
                || tollPayment.getPaymentStatus().isBlank() 
                || "CLEARED".equalsIgnoreCase(tollPayment.getPaymentStatus())) {
            tollPayment.setPaymentStatus("SUCCESS");
        }
        if (tollPayment.getPaymentTime() == null) {
            tollPayment.setPaymentTime(LocalDateTime.now());
        }
        if (tollPayment.getValidUntil() == null) {
            tollPayment.setValidUntil(tollPayment.getPaymentTime().plusHours(24));
        }
        if (tollPayment.getTransactionId() == null || tollPayment.getTransactionId().isBlank()) {
            tollPayment.setTransactionId("TXN" + System.currentTimeMillis());
        }
        return tollPaymentDAO.save(tollPayment);
    }

    @Override
    public Optional<TollPayment> findById(Long id) {
        return tollPaymentDAO.findById(id);
    }

    @Override
    public List<TollPayment> getAllPayments() {
        return tollPaymentDAO.findAll();
    }

    @Override
    public void deletePayment(Long id) {
        tollPaymentDAO.deleteById(id);
    }
}
