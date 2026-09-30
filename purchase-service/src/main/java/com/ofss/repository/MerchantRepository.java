package com.ofss.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ofss.entity.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

}