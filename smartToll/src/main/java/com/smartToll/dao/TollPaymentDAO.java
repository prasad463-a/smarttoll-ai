package com.smartToll.dao;


import java.util.List;
import java.util.Optional;

import com.smartToll.entity.TollPayment;

public interface TollPaymentDAO {

    TollPayment save(TollPayment tollPayment);

    Optional<TollPayment> findById(Long id);

    List<TollPayment> findAll();

    void deleteById(Long id);
}