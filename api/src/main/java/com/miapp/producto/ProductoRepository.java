package com.miapp.producto;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

}
