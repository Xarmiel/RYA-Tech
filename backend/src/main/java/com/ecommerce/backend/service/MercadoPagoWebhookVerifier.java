package com.ecommerce.backend.service;

import com.ecommerce.backend.config.MercadoPagoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component @RequiredArgsConstructor
public class MercadoPagoWebhookVerifier {
    private final MercadoPagoProperties config;
    public void verify(String signature,String requestId,String dataId) {
        if(config.getWebhookSecret()==null||config.getWebhookSecret().isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Webhook sin configurar");
        try {
            if(signature==null||requestId==null||requestId.isBlank()||dataId==null||!dataId.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException();
            var parts=new HashMap<String,String>();for(String part:signature.split(",")){String[] pair=part.trim().split("=",2);if(pair.length==2&&parts.put(pair[0],pair[1])!=null)throw new IllegalArgumentException();}
            String ts=parts.get("ts"), hash=parts.get("v1");if(ts==null||!ts.matches("[0-9]+")||hash==null||!hash.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException();
            String manifest="id:"+dataId.toLowerCase(Locale.ROOT)+";request-id:"+requestId+";ts:"+ts+";";
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(config.getWebhookSecret().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            if(!MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(hash)))throw new IllegalArgumentException();
        }catch(Exception ex){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Firma de webhook inválida");}
    }
}
