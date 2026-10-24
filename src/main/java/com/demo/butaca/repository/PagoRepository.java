package com.demo.butaca.repository;

import com.demo.butaca.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByCompraIdOrderByFechaCreacionPagoDesc(Long idCompra);
    Optional<Pago> findFirstByCompraIdOrderByFechaCreacionPagoDesc(Long idCompra);
}
