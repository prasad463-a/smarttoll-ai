package com.smartToll.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity

public class TollPayment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	 @ManyToOne
	 private Vehicle vehicle;
	 
	 @ManyToOne
	 private Toll toll;
	 
	 private Double amount;
	 @Column(unique = true)
	 private String transactionId;
	 
	 private String paymentStatus;
	 
	 private LocalDateTime paymentTime;
	 private LocalDateTime validUntil;
	 
	

}
