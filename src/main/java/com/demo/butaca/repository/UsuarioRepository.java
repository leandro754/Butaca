package com.demo.butaca.repository;

import com.demo.butaca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmailUsuarioIgnoreCase(String emailUsuario);

    Optional<Usuario> findByEmailUsuarioIgnoreCase(String emailUsuario);

    Optional<Usuario> findByDniUsuario(String dniUsuario);
}
