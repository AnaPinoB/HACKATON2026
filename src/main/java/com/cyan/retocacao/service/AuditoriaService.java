package com.cyan.retocacao.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditoriaService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public AuditoriaService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public record Cambio(long id, String usuario, String accion, String tipoRegistro, String registroId,
        String campo, String valorAnterior, String valorNuevo, String motivo, LocalDateTime fecha) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String accion, String tipo, String id, String campo, Object anterior, Object nuevo, String motivo) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException("Se requiere identidad autenticada");
        }
        jdbc.update("""
            INSERT INTO auditoria_cacao
            (usuario_encargado, accion, tipo_registro, registro_id, campo_modificado, valor_anterior, valor_nuevo, motivo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, auth.getName(), accion, tipo, id, campo,
            anterior == null ? null : json.writeValueAsString(anterior),
            nuevo == null ? null : json.writeValueAsString(nuevo), motivo);
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public List<Cambio> porLote(String codigo) {
        return jdbc.query("""
            SELECT * FROM auditoria_cacao a
            WHERE (a.tipo_registro='LOTE' AND a.registro_id=?)
               OR (a.tipo_registro='OPERACION' AND a.registro_id IN
                   (SELECT codigo_operacion FROM registro_operaciones WHERE lote_codigo=?))
            ORDER BY a.id_auditoria
            """, (rs, row) -> new Cambio(rs.getLong("id_auditoria"), rs.getString("usuario_encargado"),
            rs.getString("accion"), rs.getString("tipo_registro"), rs.getString("registro_id"),
            rs.getString("campo_modificado"), rs.getString("valor_anterior"), rs.getString("valor_nuevo"),
            rs.getString("motivo"), rs.getTimestamp("fecha_cambio").toLocalDateTime()), codigo, codigo);
    }
}
