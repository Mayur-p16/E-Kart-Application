package com.jsp.ekart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jsp.ekart.dto.Item;

public interface ItemRepository extends JpaRepository<Item, Integer>{

}
