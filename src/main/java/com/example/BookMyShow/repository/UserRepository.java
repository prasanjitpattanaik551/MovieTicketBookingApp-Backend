package com.example.BookMyShow.repository;

import com.example.BookMyShow.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User,Long>
{
    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

}
