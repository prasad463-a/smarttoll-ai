package com.smartToll.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartToll.entity.TollPayment;
import com.smartToll.service.TollPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class TollPaymentController {

    private final TollPaymentService tollPaymentService;

    @PostMapping("/add")
    public TollPayment addPayment(@RequestBody TollPayment tollPayment) {
        return tollPaymentService.addPayment(tollPayment);
    }

    @GetMapping("/{id}")
    public Optional<TollPayment> findById(@PathVariable Long id) {
        return tollPaymentService.findById(id);
    }

    @GetMapping("/getall")
    public List<TollPayment> getAllPayments() {
        return tollPaymentService.getAllPayments();
    }

    @DeleteMapping("/{id}")
    public String deletePayment(@PathVariable Long id) {

        tollPaymentService.deletePayment(id);

        return "Payment deleted successfully";
    }
}
