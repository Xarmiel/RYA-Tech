package com.ecommerce.backend.technicaldraft;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.core.MethodParameter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@RestControllerAdvice(assignableTypes=TechnicalDraftController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TechnicalDraftAdvice extends RequestBodyAdviceAdapter {
    @Override public boolean supports(MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter){return parameter.getContainingClass()==TechnicalDraftController.class;}
    @Override public HttpInputMessage beforeBodyRead(HttpInputMessage input,MethodParameter parameter,Type target,Class<? extends HttpMessageConverter<?>> converter) throws IOException {
        byte[] body=input.getBody().readNBytes(131073);
        if(body.length>131072)throw new DraftFault(413,"La solicitud supera 128 KiB.");
        return new HttpInputMessage(){public InputStream getBody(){return new ByteArrayInputStream(body);}public HttpHeaders getHeaders(){return input.getHeaders();}};
    }
    @ExceptionHandler(DraftFault.class) public ResponseEntity<?> fault(DraftFault ex){return ResponseEntity.status(ex.status).body(Map.of("status",ex.status,"message",ex.getMessage(),"issues",ex.issues));}
    @ExceptionHandler(OptimisticLockingFailureException.class) public ResponseEntity<?> conflict(OptimisticLockingFailureException ex){return ResponseEntity.status(409).body(Map.of("status",409,"message","El borrador cambió en otra sesión. Recarga antes de continuar."));}
    @ExceptionHandler(HttpMessageNotReadableException.class) public ResponseEntity<?> malformed(HttpMessageNotReadableException ex){return ResponseEntity.badRequest().body(Map.of("status",400,"message","JSON de solicitud inválido."));}
}
