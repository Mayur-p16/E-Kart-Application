package com.jsp.ekart.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsp.ekart.dto.Customer;
import com.jsp.ekart.dto.Order;

public interface OrderRepository extends JpaRepository<Order, Integer>{

	List<Order> findByCustomer(Customer customer);

}
