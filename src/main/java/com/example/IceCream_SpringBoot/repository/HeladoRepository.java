package com.example.IceCream_SpringBoot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.IceCream_SpringBoot.model.HeladoDocument;

public interface HeladoRepository extends MongoRepository<HeladoDocument, String> {
    List<HeladoDocument> findByUbicacion(String ubicacion);
    List<HeladoDocument> findByUbicacionIn(List<String> ubicaciones);
    List<HeladoDocument> findByNombreIgnoreCase(String nombre);
    boolean existsByNombre(String nombre);
    Optional<HeladoDocument> findByNombreIgnoreCaseAndUbicacionIgnoreCase(String nombre, String ubicacion);
    Optional<HeladoDocument> findByNombreAndUbicacion(String nombre, String ubicacion);
}
