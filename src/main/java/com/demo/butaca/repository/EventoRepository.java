package com.demo.butaca.repository;

import com.demo.butaca.model.EstadoEvento;
import com.demo.butaca.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByEstadoPublicacionEventoOrderByFechaEventoAsc(EstadoEvento estadoPublicacionEvento);
    List<Evento> findByOrganizadorIdUsuario(Long idUsuario);
    List<Evento> findByCategoriaIdCategoria(Long categoriaId);
    long countByEstadoPublicacionEvento(EstadoEvento estadoPublicacionEvento);

    @Query("select e from Evento e where e.idEvento = :id")
    Optional<Evento> findByIdParaCompra(@Param("id") Long id);
}
