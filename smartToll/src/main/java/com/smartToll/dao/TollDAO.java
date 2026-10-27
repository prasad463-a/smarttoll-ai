package com.smartToll.dao;

import java.util.List;
import java.util.Optional;

import com.smartToll.entity.Toll;

public interface TollDAO {

    Toll save(Toll toll);

    Optional<Toll> findById(Long id);

    List<Toll> findAll();

    void deleteById(Long id);
}