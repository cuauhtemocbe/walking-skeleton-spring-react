package com.miapp.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRfc(String rfc);

}
