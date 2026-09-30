package com.ofss.customer.repository;

import com.ofss.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailAddress(String emailAddress);

    Optional<Customer> findByMobileNumber(String mobileNumber);

    Optional<Customer> findByPanNumber(String panNumber);
}