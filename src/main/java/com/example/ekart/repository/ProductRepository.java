package com.example.ekart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ekart.dto.Product;
import com.example.ekart.dto.Vendor;


public interface ProductRepository extends JpaRepository<Product, Integer> {

	List<Product> findByVendor(Vendor vendor);

	List<Product> findByNameLike(String toSearch);

	List<Product> findByDescriptionLike(String toSearch);

	List<Product> findByCategoryLike(String toSearch);

	List<Product> findByApprovedTrue();

}
