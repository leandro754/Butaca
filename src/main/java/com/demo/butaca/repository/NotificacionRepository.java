package com.demo.butaca.repository;

import com.demo.butaca.model.Notificacion;
import com.demo.butaca.model.EstadoEnvioNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    boolean existsByCompraIdAndTipo(Long compraId, String tipo);
    List<Notificacion> findByCompraIdOrderByCreatedAtDesc(Long compraId);
    List<Notificacion> findTop20ByEstadoEnvioOrderByCreatedAtAsc(EstadoEnvioNotificacion estado);
    List<Notificacion> findByUsuarioIdUsuarioOrderByCreatedAtDesc(Long usuarioId);

    @Query("select n from Notificacion n where n.usuario.idUsuario = :usuarioId order by n.createdAt desc")
    List<Notificacion> findByUsuarioIdOrderByFechaEnvioDesc(@Param("usuarioId") Long usuarioId);

    @Query("select n from Notificacion n where n.evento.idEvento = :eventoId order by n.fechaEnvio desc")
    List<Notificacion> findByEventoId(@Param("eventoId") Long eventoId);

    @Query("select n from Notificacion n where n.usuario.idUsuario = :usuarioId order by n.fechaEnvio desc")
    List<Notificacion> findByUsuarioId(@Param("usuarioId") Long usuarioId);
}
