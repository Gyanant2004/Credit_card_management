package com.ofss.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ofss.dto.MerchantRequest;
import com.ofss.entity.Merchant;
import com.ofss.exception.ResourceNotFoundException;
import com.ofss.repository.MerchantRepository;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public MerchantService(MerchantRepository merchantRepository) {
        this.merchantRepository = merchantRepository;
    }

    public Merchant addMerchant(MerchantRequest request) {

        Merchant merchant = new Merchant();

        merchant.setMerchantName(request.getMerchantName());
        merchant.setCategory(request.getCategory());
        merchant.setLocation(request.getLocation());

        return merchantRepository.save(merchant);
    }

    public Merchant getMerchantById(Long merchantId) {

        return merchantRepository.findById(merchantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Merchant not found with ID: " + merchantId));
    }

    public List<Merchant> getAllMerchants() {

        return merchantRepository.findAll();
    }

    public Merchant updateMerchant(
            Long merchantId,
            MerchantRequest request) {

        Merchant merchant = getMerchantById(merchantId);

        merchant.setMerchantName(request.getMerchantName());
        merchant.setCategory(request.getCategory());
        merchant.setLocation(request.getLocation());

        return merchantRepository.save(merchant);
    }

    public void deleteMerchant(Long merchantId) {

        Merchant merchant = getMerchantById(merchantId);

        merchantRepository.delete(merchant);
    }
}
