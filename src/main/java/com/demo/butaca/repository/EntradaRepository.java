package com.demo.butaca.repository;

import com.demo.butaca.model.Entrada;
import com.demo.butaca.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    Optional<Entrada> findByCodigoUnicoEntrada(UUID codigoUnicoEntrada);

    @Query("select e from Entrada e where e.compra.cliente.idUsuario = :idUsuario")
    List<Entrada> findByClienteIdUsuario(@Param("idUsuario") Long idUsuario);

    List<Entrada> findByEventoIdEvento(Long idEvento);
    List<Entrada> findByCompraId(Long compraId);
    List<Entrada> findByEventoIdEventoAndEstadoEntrada(Long idEvento, EstadoEntrada estadoEntrada);
    long countByEventoIdEventoAndEstadoEntradaIn(Long idEvento, Collection<EstadoEntrada> estados);
}
