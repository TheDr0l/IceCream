package com.example.IceCream_SpringBoot.repository;

import com.example.IceCream_SpringBoot.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
}
