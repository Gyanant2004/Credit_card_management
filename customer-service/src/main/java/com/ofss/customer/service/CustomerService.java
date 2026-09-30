package com.ofss.customer.service;

import com.ofss.customer.dto.CustomerRequest;
import com.ofss.customer.dto.CustomerResponse;
import com.ofss.customer.entity.Customer;
import com.ofss.customer.exception.CustomerAlreadyExistsException;
import com.ofss.customer.exception.CustomerNotFoundException;
import com.ofss.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateUniqueFields(request, null);

        Customer customer = new Customer();
        customer.setCustomerName(request.getCustomerName().trim());
        customer.setEmailAddress(request.getEmailAddress().trim().toLowerCase());
        customer.setMobileNumber(request.getMobileNumber().trim());
        customer.setPanNumber(request.getPanNumber().trim().toUpperCase());

        return mapToResponse(customerRepository.save(customer));
    }

    public CustomerResponse getCustomerById(Long customerId) {
        return mapToResponse(findCustomer(customerId));
    }

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        Customer customer = findCustomer(customerId);
        validateUniqueFields(request, customerId);

        customer.setCustomerName(request.getCustomerName().trim());
        customer.setEmailAddress(request.getEmailAddress().trim().toLowerCase());
        customer.setMobileNumber(request.getMobileNumber().trim());
        customer.setPanNumber(request.getPanNumber().trim().toUpperCase());

        return mapToResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deleteCustomer(Long customerId) {
        customerRepository.delete(findCustomer(customerId));
    }

    private Customer findCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private void validateUniqueFields(CustomerRequest request, Long currentCustomerId) {
        checkDuplicate(
                customerRepository.findByEmailAddress(request.getEmailAddress().trim().toLowerCase()),
                currentCustomerId,
                "Email address already exists"
        );

        checkDuplicate(
                customerRepository.findByMobileNumber(request.getMobileNumber().trim()),
                currentCustomerId,
                "Mobile number already exists"
        );

        checkDuplicate(
                customerRepository.findByPanNumber(request.getPanNumber().trim().toUpperCase()),
                currentCustomerId,
                "PAN number already exists"
        );
    }

    private void checkDuplicate(
            Optional<Customer> existingCustomer,
            Long currentCustomerId,
            String message
    ) {
        if (existingCustomer.isPresent()
                && !existingCustomer.get().getCustomerId().equals(currentCustomerId)) {
            throw new CustomerAlreadyExistsException(message);
        }
    }

    private CustomerResponse mapToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();

        response.setCustomerId(customer.getCustomerId());
        response.setCustomerName(customer.getCustomerName());
        response.setEmailAddress(customer.getEmailAddress());
        response.setMobileNumber(customer.getMobileNumber());
        response.setPanNumber(customer.getPanNumber());

        return response;
    }
}