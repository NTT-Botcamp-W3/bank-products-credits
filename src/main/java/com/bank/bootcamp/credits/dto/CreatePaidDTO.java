package com.bank.bootcamp.credits.dto;

import lombok.Data;

@Data
public class CreatePaidDTO {
  private String creditId;
  private Double amount;
}
