package com.smartToll.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.smartToll.dto.TollLoginRequest;
import com.smartToll.dto.TollResponse;
import com.smartToll.entity.Toll;
import com.smartToll.service.TollService;
import com.smartToll.util.TollDtoConverter;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/toll")
@RequiredArgsConstructor
public class TollController {

    private final TollService tollService;

    // Toll registration
    @PostMapping("/add")
    public ResponseEntity<TollResponse> addToll(
            @RequestBody Toll toll) {

        Toll savedToll = tollService.addToll(toll);

        return ResponseEntity.ok(
                TollDtoConverter.convertToResponse(savedToll)
        );
    }

    // Toll login
    @PostMapping("/login")
    public ResponseEntity<TollResponse> loginToll(
            @RequestBody TollLoginRequest request) {

        return ResponseEntity.ok(
                tollService.loginToll(request)
        );
    }

    // Get toll by ID
    @GetMapping("/{id}")
    public ResponseEntity<Toll> getTollById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                tollService.getTollById(id)
        );
    }

    // Get all tolls
    @GetMapping("/getall")
    public ResponseEntity<List<Toll>> getAllTolls() {

        return ResponseEntity.ok(
                tollService.getAllTolls()
        );
    }

    // Delete toll
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteToll(
            @PathVariable Long id) {

        tollService.deleteToll(id);

        return ResponseEntity.ok("Toll deleted successfully");
    }
}