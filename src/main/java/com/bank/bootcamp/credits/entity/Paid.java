package com.bank.bootcamp.credits.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "Paids")
@Data
public class Paid {
  
  @Id
  private String id;
  
  private String creditId;
  private LocalDateTime createdDate;
  private Double amount;

}
