package com.demo.butaca.repository;

import com.demo.butaca.model.TipoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {
    List<TipoEntrada> findByEventoIdEventoOrderByIdTipoEntradaAsc(Long idEvento);
    Optional<TipoEntrada> findFirstByEventoIdEventoAndActivoTipoEntradaTrueOrderByIdTipoEntradaAsc(Long idEvento);
}
