package com.bank.bootcamp.credits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import com.bank.bootcamp.credits.service.CreditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class CreditsApplicationTests {

  private static CreditService creditService;
  private static CreditRepository creditRepository;
  private static PaidRepository paidRepository;
  private static ConsumptiomRepository consumptiomRepository;

  private static ObjectMapper mapper = new ObjectMapper();

  @BeforeAll
  public static void setup() {
    creditRepository = mock(CreditRepository.class);
    paidRepository = mock(PaidRepository.class);
    consumptiomRepository = mock(ConsumptiomRepository.class);
    creditService = new CreditService(creditRepository, paidRepository, consumptiomRepository);
    mapper.registerModule(new JavaTimeModule());
  }
  
  private Credit getPersonalCredit() {
    var credit = new Credit();
    credit.setCreditType(CreditType.PERSONAL);
    credit.setCustomerId("c1");
    credit.setLimit(100d);
    return credit;
  }
  
  private Credit getBusinessCredit() {
    var credit = new Credit();
    credit.setCreditType(CreditType.BUSINESS);
    credit.setCustomerId("b1");
    credit.setLimit(1000d);
    return credit;
  }

  @Test
  public void createCreditPersonalProduct() throws Exception {
    //Personal: solo se permite un solo crédito por persona.
    var credit = getPersonalCredit();
    
    var savedCredit = mapper.readValue(mapper.writeValueAsString(credit), Credit.class);
    savedCredit.setId(UUID.randomUUID().toString());
    
    when(creditRepository.findAllByCustomerIdAndCreditType(credit.getCustomerId(), credit.getCreditType())).thenReturn(Flux.empty());
    when(creditRepository.save(credit)).thenReturn(Mono.just(savedCredit));
    
    var mono = creditService.createProduct(credit);
    StepVerifier.create(mono).assertNext(cred -> {
      assertThat(cred.getId()).isNotNull();
    }).verifyComplete();
    
    var credit2 = getPersonalCredit();
    credit2.setLimit(200d);
    
    var savedCredit2 = mapper.readValue(mapper.writeValueAsString(credit2), Credit.class);
    savedCredit2.setId(UUID.randomUUID().toString());
    
    when(creditRepository.findAllByCustomerIdAndCreditType(credit2.getCustomerId(), credit2.getCreditType())).thenReturn(Flux.just(savedCredit));
    when(creditRepository.save(credit2)).thenReturn(Mono.just(savedCredit2));
    
    var mono2 = creditService.createProduct(credit);
    StepVerifier.create(mono2).expectError(BankValidationException.class).verify();
    
  }
  
  @Test
  public void createCreditBusinessProduct() throws Exception {
    //Empresarial: se permite más de un crédito por empresa.
    var credit = getBusinessCredit();
    
    var savedCredit = mapper.readValue(mapper.writeValueAsString(credit), Credit.class);
    savedCredit.setId(UUID.randomUUID().toString());
    
    when(creditRepository.findAllByCustomerIdAndCreditType(credit.getCustomerId(), credit.getCreditType())).thenReturn(Flux.empty());
    when(creditRepository.save(credit)).thenReturn(Mono.just(savedCredit));
    
    var mono = creditService.createProduct(credit);
    StepVerifier.create(mono).assertNext(cred -> {
      assertThat(cred.getId()).isNotNull();
    }).verifyComplete();
    
    var credit2 = getBusinessCredit();
    credit2.setLimit(2000d);
    
    var savedCredit2 = mapper.readValue(mapper.writeValueAsString(credit2), Credit.class);
    savedCredit2.setId(UUID.randomUUID().toString());
    
    when(creditRepository.findAllByCustomerIdAndCreditType(credit2.getCustomerId(), credit2.getCreditType())).thenReturn(Flux.just(savedCredit));
    when(creditRepository.save(credit2)).thenReturn(Mono.just(savedCredit2));
    
    var mono2 = creditService.createProduct(credit);
    StepVerifier.create(mono2).assertNext(cred -> {
      assertThat(cred.getId()).isNotNull();
    }).verifyComplete();
    
  }
  
  @Test
  public void generateConsumptionForCredit() throws Exception {
    // Un cliente puede cargar consumos a sus tarjetas de crédito en base a su límite de crédito.
    var credit = getPersonalCredit();
    credit.setId("123");
    
    var consumption = new Consumptiom();
    consumption.setAmount(50d);
    consumption.setCreditId(credit.getId());
    consumption.setRegisterDate(LocalDateTime.now());
    
    var consumptiomDTO = new CreateConsumptiomDTO();
    consumptiomDTO.setAmount(consumption.getAmount());
    consumptiomDTO.setCreditId(consumption.getCreditId());
    
    var savedConsumption = mapper.readValue(mapper.writeValueAsString(consumption), Consumptiom.class);
    savedConsumption.setId(UUID.randomUUID().toString());
    
    when(creditRepository.findById(consumption.getCreditId())).thenReturn(Mono.just(credit));
    when(consumptiomRepository.getSumByCreditId(consumption.getCreditId())).thenReturn(Mono.just(0d));
    when(paidRepository.getSumByCreditId(consumption.getCreditId())).thenReturn(Mono.just(0d));
    when(consumptiomRepository.save(Mockito.any(Consumptiom.class))).thenReturn(Mono.just(savedConsumption));
    
    var mono = creditService.chargeConsumptiom(consumptiomDTO);
    StepVerifier.create(mono).assertNext(id -> {
      assertThat(id).isNotNull();
    }).verifyComplete();
  }
  
  @Test
  public void payCreditDebt() throws Exception {
    var credit = getPersonalCredit();
    credit.setId("123");
    
    var paid = new Paid();
    paid.setAmount(50d);
    paid.setCreditId(credit.getId());
    
    var paidDTO = new CreatePaidDTO();
    paidDTO.setAmount(paid.getAmount());
    paidDTO.setCreditId(paid.getCreditId());
    
    var savedPaid = mapper.readValue(mapper.writeValueAsString(paid), Paid.class);
    savedPaid.setId(UUID.randomUUID().toString());
    savedPaid.setCreatedDate(LocalDateTime.now());

    when(creditRepository.findById(paid.getCreditId())).thenReturn(Mono.just(credit));
    when(paidRepository.save(Mockito.any(Paid.class))).thenReturn(Mono.just(savedPaid));
    
    var mono = creditService.paidCredit(paidDTO);
    StepVerifier.create(mono).assertNext((saved) -> {
      assertThat(saved).isNotNull();
    }).verifyComplete();
  }
  
  @Test
  public void balanceCredit() {
    var credit = getPersonalCredit();
    credit.setId("123");
    
    when(creditRepository.findById(credit.getId())).thenReturn(Mono.just(credit));
    when(consumptiomRepository.getSumByCreditId(credit.getId())).thenReturn(Mono.just(50d));
    when(paidRepository.getSumByCreditId(credit.getId())).thenReturn(Mono.just(100d));
    
    var mono = creditService.getBalanceByCreditId(credit.getId());
    StepVerifier.create(mono).assertNext((dto) -> {
      assertThat(dto.getAvailable()).isEqualTo(50d);
    }).verifyComplete();
  }
  
  public void balanceByCustomerAndCreditType() {
    var customerId = "Customer001";
    var creditType = CreditType.PERSONAL;
    
    var balance = new BalanceDTO();
    balance.setAvailable(150d);
    balance.setCreditId("cred01");
    balance.setCreditLimit(200d);
    balance.setType("Credit");
    balance.setUsed(50d);
    
    var credit = new Credit();
    credit.setId("cred01");
    credit.setCustomerId(customerId);
    credit.setLimit(200d);
    
    when(creditRepository.findAllByCustomerIdAndCreditType(customerId, creditType)).thenReturn(Flux.just(credit));
    when(creditRepository.findById(credit.getId())).thenReturn(Mono.just(credit));
    when(consumptiomRepository.getSumByCreditId(credit.getId())).thenReturn(Mono.just(50d));
    when(paidRepository.getSumByCreditId(credit.getId())).thenReturn(Mono.just(0d));
    
    var flux = creditService.getBalanceByCustomerIdAndCreditType(customerId, creditType);
    
    StepVerifier.create(flux).assertNext((dto) -> {
      assertThat(dto.getAvailable()).isEqualTo(150d);
    }).verifyComplete();
    
  }

}
