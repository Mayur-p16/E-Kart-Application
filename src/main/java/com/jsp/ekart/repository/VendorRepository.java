package com.jsp.ekart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsp.ekart.dto.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {

	boolean existsByEmail(String email);

	boolean existsByMobile(long mobile);
	
	Vendor findByEmail(String email);
}
