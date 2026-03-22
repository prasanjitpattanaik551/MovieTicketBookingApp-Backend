package com.example.BookMyShow.repository;

import com.example.BookMyShow.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;



@Repository

public interface ScreenRepository extends JpaRepository<Screen,Long>
{
    List<Screen> findByTheaterId(Long theaterId);
}
