package com.ofss.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ofss.dto.MerchantRequest;
import com.ofss.entity.Merchant;
import com.ofss.service.MerchantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<Merchant> addMerchant(
            @Valid @RequestBody MerchantRequest request) {

        Merchant merchant = merchantService.addMerchant(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(merchant);
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<Merchant> getMerchant(
            @PathVariable Long merchantId) {

        return ResponseEntity.ok(
                merchantService.getMerchantById(merchantId));
    }

    @GetMapping
    public ResponseEntity<List<Merchant>> getAllMerchants() {

        return ResponseEntity.ok(
                merchantService.getAllMerchants());
    }

    @PutMapping("/{merchantId}")
    public ResponseEntity<Merchant> updateMerchant(
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantRequest request) {

        return ResponseEntity.ok(
                merchantService.updateMerchant(
                        merchantId,
                        request));
    }

    @DeleteMapping("/{merchantId}")
    public ResponseEntity<Void> deleteMerchant(
            @PathVariable Long merchantId) {

        merchantService.deleteMerchant(merchantId);

        return ResponseEntity.noContent().build();
    }
}