package com.smartToll.service;

import java.util.List;
import java.util.Optional;

import com.smartToll.entity.TollPayment;

public interface TollPaymentService {

    TollPayment addPayment(TollPayment tollPayment);

    Optional<TollPayment> findById(Long id);

    List<TollPayment> getAllPayments();

    void deletePayment(Long id);
}