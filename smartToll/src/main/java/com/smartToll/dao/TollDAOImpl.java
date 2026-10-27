package com.smartToll.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.smartToll.entity.Toll;
import com.smartToll.repository.TollRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TollDAOImpl implements TollDAO {

    private final TollRepository tollRepository;

    @Override
    public Toll save(Toll toll) {
        return tollRepository.save(toll);
    }

    @Override
    public Optional<Toll> findById(Long id) {
        return tollRepository.findById(id);
    }

    @Override
    public List<Toll> findAll() {
        return tollRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        tollRepository.deleteById(id);
    }
}