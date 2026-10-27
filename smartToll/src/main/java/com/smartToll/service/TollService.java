package com.smartToll.service;

import java.util.List;

import com.smartToll.dto.TollLoginRequest;
import com.smartToll.dto.TollResponse;
import com.smartToll.entity.Toll;

public interface TollService {

    // Register toll
    Toll addToll(Toll toll);

    // Toll login
    TollResponse loginToll(TollLoginRequest request);

    // CRUD methods
    Toll getTollById(Long id);

    List<Toll> getAllTolls();

    void deleteToll(Long id);
}