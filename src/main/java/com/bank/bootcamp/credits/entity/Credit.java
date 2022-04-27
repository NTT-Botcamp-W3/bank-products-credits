package com.bank.bootcamp.credits.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document("Credits")
@Data
public class Credit {
  
  @Id
  private String id;
  
  private CreditType creditType;
  private String customerId;
  private Double limit;
  
  private String cardNumber;
  private LocalDateTime registerDate;

}
