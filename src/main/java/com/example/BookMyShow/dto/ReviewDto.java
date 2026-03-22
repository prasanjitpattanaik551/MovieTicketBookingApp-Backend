package com.example.BookMyShow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {

    private Long id;
    private int rating;
    private String comment;
    private Long userId;
    private Long movieId;

    // getters and setters
}
