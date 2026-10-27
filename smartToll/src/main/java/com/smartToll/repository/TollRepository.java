package com.smartToll.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartToll.entity.Toll;

public interface TollRepository extends JpaRepository<Toll, Long> {

    Optional<Toll> findByEmail(String email);
}