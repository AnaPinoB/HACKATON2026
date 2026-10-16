
package com.cyan.retocacao.service;

import com.cyan.retocacao.dto.FichaPublicaDTO;
import com.cyan.retocacao.dto.FichaPublicaDTO.EtapaPublicaDTO;
import com.cyan.retocacao.repository.LoteRepository;
import com.cyan.retocacao.repository.RegistroOperacionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FichaPublicaService {

    private final LoteRepository lotes;
    private final RegistroOperacionRepository operaciones;

    public FichaPublicaService(
            LoteRepository lotes,
            RegistroOperacionRepository operaciones) {

        this.lotes = lotes;
        this.operaciones = operaciones;
    }

    @Transactional(readOnly = true)
    public FichaPublicaDTO consultar(String codigo) {

        // Buscar el lote en MySQL
        var lote = lotes.findById(codigo)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El producto no existe"
            ));

        // Obtener las etapas de transformacion
        var etapas = operaciones
            .findByLoteCodigoOrderByFechaOperacionAscIdAsc(codigo)
            .stream()
            .map(operacion -> {

                var datos = operacion.datos();

                return new EtapaPublicaDTO(
                    datos.etapa(),
                    datos.fechaOperacion(),
                    datos.cantidadEntradaKg(),
                    datos.cantidadSalidaKg()
                );
            })
            .toList();

        // Solo los productos liberados pueden ofrecerse
        boolean disponibleParaVenta =
            "LIBERADO".equalsIgnoreCase(lote.getEstadoLote());

        // Construir una respuesta exclusivamente publica
        return new FichaPublicaDTO(
            lote.getCodigo(),
            lote.getOrigen(),
            lote.getFecha(),
            lote.getCantidad(),
            lote.getUnidad(),
            lote.getEstadoLote(),
            disponibleParaVenta,
            etapas
        );
    }
}
