package com.smartToll.util;

import com.smartToll.dto.TollResponse;
import com.smartToll.entity.Toll;

public class TollDtoConverter {

    public static TollResponse convertToResponse(Toll toll) {

        TollResponse response = new TollResponse();

        response.setId(toll.getId());
        response.setName(toll.getName());
        response.setEmail(toll.getEmail());
        response.setLocation(toll.getLocation());
        response.setAmount(toll.getAmount());

        return response;
    }
}