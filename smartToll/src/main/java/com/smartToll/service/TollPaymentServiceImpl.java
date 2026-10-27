package com.smartToll.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.smartToll.dao.TollPaymentDAO;
import com.smartToll.entity.TollPayment;
import com.smartToll.service.TollPaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TollPaymentServiceImpl implements TollPaymentService {

    private final TollPaymentDAO tollPaymentDAO;

    @Override
    public TollPayment addPayment(TollPayment tollPayment) {
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