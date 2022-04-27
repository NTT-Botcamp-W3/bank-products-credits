package com.bank.bootcamp.credits.repository;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.bank.bootcamp.credits.entity.Paid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaidRepository extends ReactiveMongoRepository<Paid, String> {
  Flux<Paid> findAllByCreditId(String creditId);
  @Aggregation(pipeline = {
      "{ $match: { creditId: ?0 }}",
      "{ $group: { _id: '', total: {$sum: $amount }}}"
  })
  public Mono<Double> getSumByCreditId(String creditId);
  
}
