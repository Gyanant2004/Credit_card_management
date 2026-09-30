package com.ofss.controller;

import com.ofss.dto.AmountRequest;
import com.ofss.dto.BalanceResponse;
import com.ofss.dto.CardResponse;
import com.ofss.dto.IssueCardRequest;
import com.ofss.dto.UpdateCardRequest;
import com.ofss.service.CreditCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/cards")
public class CreditCardController {
    private final CreditCardService service;

    public CreditCardController(CreditCardService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CardResponse> issue(@Valid @RequestBody IssueCardRequest request) {
        CardResponse card = service.issue(request);
        return ResponseEntity.created(URI.create("/api/cards/" + card.cardNumber())).body(card);
    }

    @GetMapping("/{cardNumber}")
    public CardResponse get(@PathVariable Long cardNumber) {
        return service.get(cardNumber);
    }

    @GetMapping
    public List<CardResponse> list(@RequestParam(required = false) @Positive Long customerId) {
        return service.list(customerId);
    }

    @PutMapping("/{cardNumber}")
    public CardResponse update(@PathVariable Long cardNumber,
                               @Valid @RequestBody UpdateCardRequest request) {
        return service.update(cardNumber, request);
    }

    @PatchMapping("/{cardNumber}/block")
    public CardResponse block(@PathVariable Long cardNumber) {
        return service.block(cardNumber);
    }

    @PatchMapping("/{cardNumber}/unblock")
    public CardResponse unblock(@PathVariable Long cardNumber) {
        return service.unblock(cardNumber);
    }

    @PostMapping("/{cardNumber}/purchase")
    public BalanceResponse purchase(@PathVariable Long cardNumber,
                                    @Valid @RequestBody AmountRequest request) {
        return service.purchase(cardNumber, request);
    }

    @PostMapping("/{cardNumber}/payment")
    public BalanceResponse payment(@PathVariable Long cardNumber,
                                   @Valid @RequestBody AmountRequest request) {
        return service.payment(cardNumber, request);
    }
}
