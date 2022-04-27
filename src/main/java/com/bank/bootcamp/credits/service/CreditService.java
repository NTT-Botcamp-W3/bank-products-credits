package com.bank.bootcamp.credits.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import com.bank.bootcamp.credits.dto.BalanceDTO;
import com.bank.bootcamp.credits.dto.CreateConsumptiomDTO;
import com.bank.bootcamp.credits.dto.CreatePaidDTO;
import com.bank.bootcamp.credits.entity.Consumptiom;
import com.bank.bootcamp.credits.entity.Credit;
import com.bank.bootcamp.credits.entity.CreditType;
import com.bank.bootcamp.credits.entity.Paid;
import com.bank.bootcamp.credits.exception.BankValidationException;
import com.bank.bootcamp.credits.repository.ConsumptiomRepository;
import com.bank.bootcamp.credits.repository.CreditRepository;
import com.bank.bootcamp.credits.repository.PaidRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreditService {

  private final CreditRepository creditRepository;
  private final PaidRepository paidRepository;
  private final ConsumptiomRepository consumptiomRepository;
  
  public Mono<Credit> createProduct(Credit credit) {
    return Mono.just(credit)
        .then(check(credit, prd -> Optional.of(prd).isEmpty(), "Credit has not data"))
        .then(check(credit, prd -> ObjectUtils.isEmpty(prd.getCustomerId()), "Customer ID is required"))
        .then(check(credit, prd -> ObjectUtils.isEmpty(prd.getCreditType()), "Credit type is required"))
        .then(check(credit, prd -> ObjectUtils.isEmpty(prd.getLimit()), "Credit Limit is required"))
        .then(check(credit, prd -> prd.getLimit() < 0 , "Credit Limit must be greater than zero (0)"))
        .then(creditRepository.findAllByCustomerIdAndCreditType(credit.getCustomerId(), credit.getCreditType())
            .count()
            .<Long>handle((record, sink) -> {
              if (record > 0 && credit.getCreditType() == CreditType.PERSONAL)
                sink.error(new BankValidationException("Personal customer already has a credit"));
              else
                sink.complete();
            })
        )
        .then(creditRepository.save(credit));
  }
  
  private <T> Mono<Void> check(T t, Predicate<T> predicate, String messageForException) {
    return Mono.create(sink -> {
      if (predicate.test(t)) {
        sink.error(new BankValidationException(messageForException));
        return;
      } else {
        sink.success();
      }
    });
  }
  
  private Mono<Double> getCreditUsed(String creditId) {
    return consumptiomRepository.getSumByCreditId(creditId).switchIfEmpty(Mono.just(0d))
    .map(cred -> cred * -1d)
    .concatWith(paidRepository.getSumByCreditId(creditId).switchIfEmpty(Mono.just(0d)))
    .reduce(0d, (a, b) -> a + b);
  }

  public Mono<String> chargeConsumptiom(CreateConsumptiomDTO consumptionDTO) {
    return Mono.just(consumptionDTO)
        .then(check(consumptionDTO, c -> ObjectUtils.isEmpty(c), "Consumption is required"))
        .then(check(consumptionDTO, c -> ObjectUtils.isEmpty(c.getAmount()), "Consumption amount is required"))
        .then(check(consumptionDTO, c -> ObjectUtils.isEmpty(c.getCreditId()), "Consumption Credit ID is required"))
        .then(check(consumptionDTO, c -> c.getAmount() < 0, "Consumption amount must be greater than zero"))
        .then(creditRepository.findById(consumptionDTO.getCreditId()).switchIfEmpty(Mono.error(new BankValidationException("Credit not found"))))
        .flatMap(credit -> {

          var consumption = new Consumptiom();
          consumption.setAmount(consumptionDTO.getAmount());
          consumption.setCreditId(consumptionDTO.getCreditId());
          consumption.setRegisterDate(LocalDateTime.now());
          
          var x = getCreditUsed(credit.getId())
              .<Double>handle((used, sink) -> {
                var available = credit.getLimit() - used;
                if (consumption.getAmount() > available)
                  sink.error(new BankValidationException(String.format("Not enough line of credit, for charge: %d, available %d", consumption.getAmount(), available)));
                else 
                  sink.next(used);
              })
              .then(consumptiomRepository.save(consumption));
          return x;
        })
        .map(con -> con.getId());
  }

  public Mono<String> paidCredit(CreatePaidDTO paidDTO) {
    return Mono.just(paidDTO)
        .then(check(paidDTO, p -> ObjectUtils.isEmpty(p), "Paid is required"))
        .then(check(paidDTO, p -> ObjectUtils.isEmpty(p.getAmount()), "Paid Amount is required"))
        .then(check(paidDTO, p -> ObjectUtils.isEmpty(p.getCreditId()), "Paid Credit ID is required"))
        .then(creditRepository.findById(paidDTO.getCreditId()).switchIfEmpty(Mono.error(new BankValidationException("Credit not found"))))
        .flatMap(p -> {
          var paid = new Paid();
          paid.setAmount(paidDTO.getAmount());
          paid.setCreditId(paidDTO.getCreditId());
          paid.setCreditId(paidDTO.getCreditId());
          paid.setCreatedDate(LocalDateTime.now());
          return paidRepository.save(paid);
        })
        .map(p -> p.getId());
        
  }

  public Mono<BalanceDTO> getBalanceByCreditId(String creditId) {
    return Mono.just(creditId)
      .then(check(creditId, c -> ObjectUtils.isEmpty(c), "Credit ID is required"))
      .then(creditRepository.findById(creditId).switchIfEmpty(Mono.error(new BankValidationException("Credit not found"))))
      .flatMap(credit -> {
        
        var balanceDTO = new BalanceDTO();
        balanceDTO.setCreditId(credit.getId());
        balanceDTO.setCreditLimit(credit.getLimit());
        balanceDTO.setType("Credit");
        
        return getCreditUsed(credit.getId())
        .map(balance -> {
          balanceDTO.setAvailable(balance);
          balanceDTO.setUsed(balanceDTO.getCreditLimit() - balanceDTO.getAvailable());
          return balanceDTO;
        });
      });
  }

  public Flux<BalanceDTO> getBalanceByCustomerIdAndCreditType(String customerId, CreditType creditType) {
    return creditRepository.findAllByCustomerIdAndCreditType(customerId, creditType)
      .flatMap(credit -> getBalanceByCreditId(credit.getId()).flux())
     ;
  }
  
}
