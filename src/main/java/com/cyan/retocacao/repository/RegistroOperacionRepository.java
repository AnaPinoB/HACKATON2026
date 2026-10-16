package com.cyan.retocacao.repository;

import com.cyan.retocacao.model.RegistroOperacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroOperacionRepository extends JpaRepository<RegistroOperacion, Long> {
    Optional<RegistroOperacion> findByCodigoOperacion(String codigo);
    List<RegistroOperacion> findByLoteCodigoOrderByFechaOperacionAscIdAsc(String loteCodigo);
    @Query("select coalesce(sum(o.cantidadEntradaKg), 0) from RegistroOperacion o where o.loteCodigo = :codigo")
    BigDecimal cantidadConsumida(@Param("codigo") String codigo);
}
