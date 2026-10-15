
package com.Cyan.retoCacao.repository;

import com.Cyan.retoCacao.model.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteRepository extends JpaRepository<Lote, String> {
}
