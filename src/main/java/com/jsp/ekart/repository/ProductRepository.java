package com.jsp.ekart.repository;

import java.util.List;
import com.jsp.ekart.dto.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jsp.ekart.dto.Vendor;

public interface ProductRepository extends JpaRepository<Product, Integer> {
	
	
	List<Product> findByVendor(Vendor vendor);

	List<Product> findByNameLike(String toSearch);

	List<Product> findByDescriptionLike(String toSearch);

	List<Product> findByCategoryLike(String toSearch);

	List<Product> findByApprovedTrue();

}
