package com.bank.bootcamp.credits.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "Consumptioms")
@Data
public class Consumptiom {
  
  @Id
  private String id;
  
  private String creditId;
  private Double amount;
  private LocalDateTime registerDate;

}
