package com.example.IceCream_SpringBoot.repository;

import com.example.IceCream_SpringBoot.model.HeladoEliminadoDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HeladoEliminadoRepository extends MongoRepository<HeladoEliminadoDocument, String> {
}
