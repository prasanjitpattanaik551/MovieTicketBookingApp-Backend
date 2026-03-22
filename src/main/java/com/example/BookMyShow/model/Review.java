package com.example.BookMyShow.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // PRIMARY KEY

    @Column(nullable = false)
    private int rating;  // 1 to 5

    private String comment;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // FOREIGN KEY
    private User user;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false) // FOREIGN KEY
    private Movie movie;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    // getters and setters
}
