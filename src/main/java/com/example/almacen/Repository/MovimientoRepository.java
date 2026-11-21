package com.example.almacen.Repository;

import com.example.almacen.modelo.Movimiento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    @EntityGraph(attributePaths = {"usuario", "proveedor"})
    List<Movimiento> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = {"usuario", "proveedor", "detalles", "detalles.producto", "detalles.producto.categoria"})
    Optional<Movimiento> findWithDetallesById(Integer id);
}
