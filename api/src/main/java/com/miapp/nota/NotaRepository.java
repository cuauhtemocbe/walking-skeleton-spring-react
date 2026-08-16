package com.miapp.nota;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface NotaRepository extends JpaRepository<Nota, UUID> {
}
