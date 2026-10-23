package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AyudaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class AyudaService {
    private final NamedParameterJdbcTemplate jdbc;
    private static final String BASE = """
        FROM rya.preguntas_frecuentes f
        LEFT JOIN rya.categorias_soporte c ON c.id=f.categoria_id AND c.is_deleted=false
        WHERE f.is_deleted=false AND f.publicada=true
          AND (f.categoria_id IS NULL OR c.id IS NOT NULL)
        """;

    public AyudaDto preguntas(String q, UUID categoriaId, int page, int size) {
        var params = new MapSqlParameterSource("limite", size).addValue("offset", (long)page*size);
        var where = new StringBuilder(BASE);
        if (q != null && !q.isBlank()) {
            String term = Normalizer.normalize(q.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").replace("!", "!!").replace("%", "!%").replace("_", "!_");
            where.append(" AND TRANSLATE(LOWER(f.pregunta || ' ' || f.respuesta), 'áéíóúüñ', 'aeiouun') LIKE :q ESCAPE '!'");
            params.addValue("q", "%" + term + "%");
        }
        if (categoriaId != null) { where.append(" AND f.categoria_id=:categoria"); params.addValue("categoria", categoriaId); }
        long total = jdbc.queryForObject("SELECT COUNT(*) " + where, params, Long.class);
        var items = jdbc.query("SELECT f.id,f.pregunta,f.respuesta,f.categoria_id,COALESCE(c.nombre,'General') AS categoria "
                + where + " ORDER BY f.posicion,f.pregunta,f.id LIMIT :limite OFFSET :offset", params,
                (rs,row) -> new AyudaDto.Pregunta(rs.getObject("id", UUID.class), rs.getString("pregunta"),
                        rs.getString("respuesta"), rs.getObject("categoria_id", UUID.class), rs.getString("categoria")));
        var categorias = jdbc.query("SELECT DISTINCT c.id,c.nombre " + BASE + " AND c.id IS NOT NULL ORDER BY c.nombre,c.id", params,
                (rs,row) -> new AyudaDto.Categoria(rs.getObject("id", UUID.class),rs.getString("nombre")));
        return new AyudaDto(items,categorias,page,size,total,(int)Math.ceil((double)total/size));
    }
}
