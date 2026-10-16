package com.Cyan.retoCacao.repository;

import com.Cyan.retoCacao.model.Lote;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class LoteRegistroRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Lote insertar(Lote lote) {
        // persist fuerza INSERT: un registro concurrente nunca actualiza un lote existente.
        entityManager.persist(lote);
        entityManager.flush();
        // Recuperar la fecha que genera MySQL.
        entityManager.refresh(lote);
        return lote;
    }
}
