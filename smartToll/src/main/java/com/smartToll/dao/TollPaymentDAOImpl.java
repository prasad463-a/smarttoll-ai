package com.smartToll.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.smartToll.dao.TollPaymentDAO;
import com.smartToll.entity.TollPayment;
import com.smartToll.repository.TollPaymentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TollPaymentDAOImpl implements TollPaymentDAO {

    private final TollPaymentRepository tollPaymentRepository;

    @Override
    public TollPayment save(TollPayment tollPayment) {
        return tollPaymentRepository.save(tollPayment);
    }

    @Override
    public Optional<TollPayment> findById(Long id) {
        return tollPaymentRepository.findById(id);
    }

    @Override
    public List<TollPayment> findAll() {
        return tollPaymentRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        tollPaymentRepository.deleteById(id);
    }
}
