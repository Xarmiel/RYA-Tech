package com.ecommerce.backend.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import com.ecommerce.backend.exception.ConflictException;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AdminContentService {
 private final AdminOperationsService ops;private final JdbcTemplate jdbc;
 public Map<String,Object> list(String type,int page){if("FAQ".equals(type))return ops.page("SELECT id,pregunta AS titulo,respuesta AS texto,categoria_id,posicion,publicada,revision_admin AS version,created_admin_at AS created_at","FROM rya.preguntas_frecuentes ","WHERE is_deleted=false",page);return ops.page("SELECT id,titulo,texto,tipo,ubicacion,enlace,imagen_url,posicion,publicada,version,created_at","FROM rya.contenido_editorial ","WHERE tipo=?",page,type);}
 public List<Map<String,Object>> categories(){return ops.rows("SELECT id,nombre FROM rya.categorias_soporte WHERE is_deleted=false ORDER BY nombre");}
 @Transactional public UUID save(UUID id,String type,String title,String text,String place,UUID category,int position,boolean published,String link,String image,long version,UUID actor){AdminOperationsService.safeUrl(link);AdminOperationsService.safeUrl(image);if(id==null){id=UUID.randomUUID();if("FAQ".equals(type))jdbc.update("INSERT INTO rya.preguntas_frecuentes(id,pregunta,respuesta,categoria_id,posicion,publicada) VALUES (?,?,?,?,?,?)",id,title,text,category,position,published);else jdbc.update("INSERT INTO rya.contenido_editorial(id,tipo,titulo,texto,ubicacion,enlace,imagen_url,posicion,publicada) VALUES (?,?,?,?,?,?,?,?,?)",id,type,title,text,place,link,image,position,published);}else{int count;if("FAQ".equals(type))count=jdbc.update("UPDATE rya.preguntas_frecuentes SET pregunta=?,respuesta=?,categoria_id=?,posicion=?,publicada=?,revision_admin=revision_admin+1 WHERE id=? AND is_deleted=false AND revision_admin=?",title,text,category,position,published,id,version);else count=jdbc.update("UPDATE rya.contenido_editorial SET titulo=?,texto=?,ubicacion=?,enlace=?,imagen_url=?,posicion=?,publicada=?,version=version+1 WHERE id=? AND tipo=? AND version=?",title,text,place,link,image,position,published,id,type,version);if(count!=1)throw new ConflictException("El contenido cambió. Actualiza el listado.");}ops.audit(actor,"contenido",id.toString(),published?"PUBLICADO":"BORRADOR",title);return id;}
 public Map<String,Object> contact(){return ops.require("SELECT email,telefono,whatsapp,horario,version FROM rya.contacto_tienda WHERE id=1").getFirst();}
 @Transactional public void contact(String email,String phone,String whatsapp,String hours,long version,UUID actor){if(jdbc.update("UPDATE rya.contacto_tienda SET email=?,telefono=?,whatsapp=?,horario=?,version=version+1 WHERE id=1 AND version=?",email,phone,whatsapp,hours,version)!=1)throw new ConflictException("El contacto cambió. Actualiza los datos.");ops.audit(actor,"contenido","contacto","ACTUALIZADO","Canales de contacto y horario");}
 public List<Map<String,Object>> published(String location){return ops.rows("SELECT id,titulo,texto,tipo,enlace,imagen_url FROM rya.contenido_editorial WHERE publicada=true AND ubicacion=? ORDER BY posicion,id LIMIT 30",location);}
}
