package com.ecommerce.backend.service;
import com.ecommerce.backend.config.MercadoPagoProperties;
import com.ecommerce.backend.entity.ClientePago;
import com.ecommerce.backend.repository.ClientePagoRepository;
import com.ecommerce.backend.exception.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service @RequiredArgsConstructor
public class TarjetasPagoService {
 private final MercadoPagoOrdersClient client;
 private final MercadoPagoProperties config;
 private final ClientePagoRepository repository;
 private final PlatformTransactionManager transactions;
 public record Card(String id,String lastFour,String brand,String paymentMethodId,String paymentTypeId,int expirationMonth,int expirationYear,int securityCodeLength) {}
 public record OwnedCard(String customerId,Card card) {}
 private String scope(){try{return (config.isLiveMode()?"live:":"test:")+HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(config.getPublicKey().getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 private UUID owner(UUID user){if(user==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Inicia sesión para consultar tus tarjetas.");return user;}
 public List<Card> list(UUID user){
  client.requireCredentials();var mapping=repository.findByUsuarioIdAndEntorno(owner(user),scope());
  if(mapping.isEmpty()||mapping.get().getCustomerId()==null)return List.of();
  var nodes=client.cards(mapping.get().getCustomerId());if(nodes==null||!nodes.isArray())throw unavailable();
  var cards=new ArrayList<Card>();for(var node:nodes){var card=card(node);if(card!=null)cards.add(card);}return cards;
 }
 public OwnedCard owned(UUID user,String cardId){
  var mapping=repository.findByUsuarioIdAndEntorno(owner(user),scope()).orElseThrow(()->new ResourceNotFoundException("Tarjeta no encontrada en tu cuenta."));
  var selected=list(user).stream().filter(c->c.id().equals(cardId)).findFirst().orElseThrow(()->new ResourceNotFoundException("Tarjeta no encontrada en tu cuenta."));
  var now=java.time.YearMonth.now();if(selected.expirationYear()<now.getYear()||(selected.expirationYear()==now.getYear()&&selected.expirationMonth()<now.getMonthValue()))throw new BadRequestException("La tarjeta está vencida. Agrega otra tarjeta.");
  return new OwnedCard(mapping.getCustomerId(),selected);
 }
 public Card save(UUID user,String email,String token,boolean consent){
  client.requireCredentials();owner(user);
  if(!consent)throw new BadRequestException("Autoriza el guardado de la tarjeta para continuar.");
  if(email==null||email.isBlank())throw new BadRequestException("Tu cuenta debe tener un correo electrónico confirmado.");
  // Persist the owner binding first. Never discover a customer by an editable checkout email.
  var existing=repository.findByUsuarioIdAndEntorno(user,scope());
  UUID mappingId=existing.map(ClientePago::getId).orElseGet(()->{
   var binding=new ClientePago();binding.setId(UUID.randomUUID());binding.setUsuarioId(user);binding.setEntorno(scope());
   try{return new TransactionTemplate(transactions).execute(status->repository.saveAndFlush(binding).getId());}
   catch(org.springframework.dao.DataIntegrityViolationException e){return repository.findByUsuarioIdAndEntorno(user,scope()).orElseThrow(()->e).getId();}
  });
  try{new TransactionTemplate(transactions).executeWithoutResult(status->{
   var mapping=repository.lock(mappingId).orElseThrow();
   if(mapping.getCustomerId()==null){var customer=client.customerCreate(email);String id=customer==null?"":customer.path("id").asText();if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw unavailable();mapping.setCustomerId(id);repository.saveAndFlush(mapping);}
  });
   var mapping=repository.findById(mappingId).orElseThrow();
   var saved=card(client.saveCard(mapping.getCustomerId(),token));if(saved==null)throw unavailable();return saved;
  }catch(org.springframework.web.client.RestClientException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No se confirmó el guardado en Mercado Pago. Actualiza tus tarjetas antes de volver a intentarlo.");}
 }
 private Card card(JsonNode node){
  if(node==null)return null;var method=node.path("payment_method");String id=node.path("id").asText(""),last=node.path("last_four_digits").asText(""),type=method.path("payment_type_id").asText("");
  if(!id.matches("[A-Za-z0-9_-]{1,100}")||!last.matches("[0-9]{4}")||!Set.of("credit_card","debit_card").contains(type))return null;
  if(node.has("live_mode")&&node.path("live_mode").asBoolean()!=config.isLiveMode())return null;
  return new Card(id,last,method.path("name").asText("Tarjeta"),method.path("id").asText(),type,node.path("expiration_month").asInt(),node.path("expiration_year").asInt(),node.path("security_code").path("length").asInt(3));
 }
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Mercado Pago no confirmó los datos de la tarjeta. Consulta tus tarjetas antes de repetir.");}
}
