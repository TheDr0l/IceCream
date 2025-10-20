package com.example.IceCream_SpringBoot.repository;

import com.example.IceCream_SpringBoot.model.VentaDocument;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends MongoRepository<VentaDocument, String> {
    List<VentaDocument> findByMetodoPago(String metodoPago);
    List<VentaDocument> findAllByOrderByFechaVentaDesc();
    Page<VentaDocument> findAllByOrderByFechaVentaDesc(Pageable pageable);
    Page<VentaDocument> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

}
