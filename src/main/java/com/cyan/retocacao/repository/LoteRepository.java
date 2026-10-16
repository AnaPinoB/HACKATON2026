
package com.cyan.retocacao.repository;

import com.cyan.retocacao.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface LoteRepository extends JpaRepository<Lote, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Lote l where l.codigo = :codigo")
    Optional<Lote> bloquearPorCodigo(@Param("codigo") String codigo);
}
