package com.bank.bootcamp.credits.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bank.bootcamp.credits.dto.BalanceDTO;
import com.bank.bootcamp.credits.dto.CreateConsumptiomDTO;
import com.bank.bootcamp.credits.dto.CreatePaidDTO;
import com.bank.bootcamp.credits.entity.Credit;
import com.bank.bootcamp.credits.entity.CreditType;
import com.bank.bootcamp.credits.service.CreditService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/credits")
@RequiredArgsConstructor
public class CreditController {

  private final CreditService creditService;
  
  @PostMapping
  public Mono<String> create(@RequestBody Credit credit) {
    return creditService.createProduct(credit).map(cred -> cred.getId());
  }
  
  @PostMapping("/consumptiom")
  public Mono<String> chargeConsumptiom(@RequestBody CreateConsumptiomDTO consumptiom) {
    return creditService.chargeConsumptiom(consumptiom);
  }
  
  @PostMapping("/paid")
  public Mono<String> paidCredit(@RequestBody CreatePaidDTO paid) {
    return creditService.paidCredit(paid);
  }
  
  @GetMapping("/balanceById/{creditId}")
  public Mono<BalanceDTO> balanceByCreditId(@PathVariable("creditId") String creditId) {
    return creditService.getBalanceByCreditId(creditId);
  }
  
  @GetMapping("/balanceByCustomer/{customerId}/{creditType}")
  public Flux<BalanceDTO> balanceByCustomerIdAndCreditType(@PathVariable("customerId") String customerId, @PathVariable("creditType") CreditType creditType) {
    return creditService.getBalanceByCustomerIdAndCreditType(customerId, creditType);
  }
  
}
