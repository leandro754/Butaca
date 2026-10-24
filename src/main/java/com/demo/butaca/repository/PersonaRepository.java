package com.demo.butaca.repository;

import com.demo.butaca.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {
    Optional<Persona> findByDniPersona(Long dniPersona);
}
