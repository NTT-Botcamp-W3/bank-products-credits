package com.bank.bootcamp.credits.dto;

import lombok.Data;

@Data
public class BalanceDTO {

  private String creditId;
  private String type;
  private Double creditLimit;
  private Double used;
  private Double available;
}
