package com.demo.butaca.repository;

import com.demo.butaca.model.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.demo.butaca.model.EstadoEnvio;
import java.util.List;

import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    @EntityGraph(attributePaths = {"pago", "pago.compra", "pago.compra.cliente"})
    List<Comprobante> findByEstadoEnvioComprobante(EstadoEnvio estado);
    Optional<Comprobante> findByPagoIdPago(Long idPago);
    Optional<Comprobante> findByPagoCompraId(Long idCompra);
    Optional<Comprobante> findByNumeroComprobante(String numeroComprobante);
}
