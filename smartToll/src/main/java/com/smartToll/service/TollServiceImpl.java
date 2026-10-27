package com.smartToll.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.smartToll.dao.TollDAOImpl;
import com.smartToll.dto.TollLoginRequest;
import com.smartToll.dto.TollResponse;
import com.smartToll.entity.Toll;
import com.smartToll.jwt.JwtUtil;
import com.smartToll.repository.TollRepository;
import com.smartToll.util.TollDtoConverter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TollServiceImpl implements TollService {

    private final TollDAOImpl tollDAO;
    private final TollRepository tollRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Register toll
    @Override
    public Toll addToll(Toll toll) {

        if (tollRepository.findByEmail(toll.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        // Encrypt password before saving
        toll.setPassword(
                passwordEncoder.encode(toll.getPassword())
        );

        return tollDAO.save(toll);
    }

    // Toll login with JWT
    @Override
    public TollResponse loginToll(TollLoginRequest request) {

        // Find toll by email
        Toll toll = tollRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        // Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                toll.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        // Generate JWT
        String token = jwtUtil.generateToken(toll.getEmail());

        // Prepare response
        TollResponse response =
                TollDtoConverter.convertToResponse(toll);

        response.setToken(token);

        return response;
    }

    // Get toll by ID
    @Override
    public Toll getTollById(Long id) {

        return tollDAO.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Toll not found")
                );
    }

    // Get all tolls
    @Override
    public List<Toll> getAllTolls() {
        return tollDAO.findAll();
    }

    // Delete toll
    @Override
    public void deleteToll(Long id) {
        tollDAO.deleteById(id);
    }
}