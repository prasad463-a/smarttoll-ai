package com.smartToll.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	
	@Column(unique = true, nullable = false)
	private String email;

	private String password;
	private String phone;
	private String role;
	
	@OneToMany(mappedBy = "user")
	@JsonIgnore
	@ToString.Exclude
    private List<Vehicle> vehicles;
	

}
