package com.bank.bootcamp.credits.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.bank.bootcamp.credits.entity.Credit;
import com.bank.bootcamp.credits.entity.CreditType;
import reactor.core.publisher.Flux;

public interface CreditRepository extends ReactiveMongoRepository<Credit, String> {
  Flux<Credit> findAllByCustomerIdAndCreditType(String customerId, CreditType creditType);
}
