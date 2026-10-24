package com.demo.butaca.repository;

import com.demo.butaca.model.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReembolsoRepository extends JpaRepository<Reembolso, Long> {
    List<Reembolso> findByPagoIdPago(Long idPago);
}
