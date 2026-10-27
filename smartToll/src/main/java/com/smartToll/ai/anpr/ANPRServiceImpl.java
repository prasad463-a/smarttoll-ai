
package com.smartToll.ai.anpr;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.smartToll.ai.anpr.dto.ANPRRequest;
import com.smartToll.ai.anpr.dto.ANPRResponse;
import com.smartToll.entity.Vehicle;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ANPRServiceImpl implements ANPRService {

    private final RestTemplate restTemplate;
    private final ANPRVerificationService anprVerificationService;

    @Override
    public ANPRResponse processImage(ANPRRequest request) {

        // Step 1: Send image path to Python AI service
        String pythonUrl = "http://127.0.0.1:5000/anpr/process";

        ANPRResponse response = restTemplate.postForObject(
                pythonUrl,
                request,
                ANPRResponse.class
        );

        // Step 2: Check Python response
        if (response == null) {
            throw new RuntimeException(
                    "Python AI service returned an empty response"
            );
        }

        // Step 3: Check whether a number plate was detected
        String vehicleNumber = response.getVehicleNumber();

        if (!response.isVehicleDetected()
                || vehicleNumber == null
                || vehicleNumber.isBlank()) {

            response.setPaymentValid(false);
            response.setGateDecision("PLATE_NOT_DETECTED");
            response.setMessage("Number plate not detected");

            return response;
        }

        // Step 4: Find vehicle in MySQL
        Vehicle vehicle =
                anprVerificationService.findVehicle(vehicleNumber);

        if (vehicle == null) {
            response.setPaymentValid(false);
            response.setGateDecision("PAYMENT_REQUIRED");
            response.setMessage("Vehicle is not registered");

            return response;
        }

        // Step 5: Check valid toll payment
        boolean paymentValid =
                anprVerificationService.hasValidPayment(vehicle);

        // Step 6: Decide whether gate can open
        if (paymentValid) {

            response.setPaymentValid(true);
            response.setGateDecision("ALLOW");
            response.setMessage(
                    "Payment is valid. Gate can open."
            );

        } else {

            response.setPaymentValid(false);
            response.setGateDecision("PAYMENT_REQUIRED");
            response.setMessage(
                    "No valid toll payment found."
            );
        }

        return response;
    }
}