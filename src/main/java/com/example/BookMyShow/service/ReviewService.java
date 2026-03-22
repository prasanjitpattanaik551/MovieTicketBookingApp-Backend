package com.example.BookMyShow.service;

import com.example.BookMyShow.dto.ReviewDto;
import com.example.BookMyShow.exception.ResourceNotFoundException;
import com.example.BookMyShow.model.Movie;
import com.example.BookMyShow.model.Review;
import com.example.BookMyShow.model.User;
import com.example.BookMyShow.repository.MovieRepository;
import com.example.BookMyShow.repository.ReviewRepository;
import com.example.BookMyShow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    // Add review
    public ReviewDto addReview(ReviewDto reviewDto) {

        User user = userRepository.findById(reviewDto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + reviewDto.getUserId()));

        Movie movie = movieRepository.findById(reviewDto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + reviewDto.getMovieId()));

        Review review = new Review();
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setUser(user);
        review.setMovie(movie);
        review.setCreatedTime(LocalDateTime.now());

        Review savedReview = reviewRepository.save(review);

        return mapToDto(savedReview);
    }

    // Get reviews by movie
    public List<ReviewDto> getReviewsByMovie(Long movieId) {
        return reviewRepository.findByMovieId(movieId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Get reviews by user
    public List<ReviewDto> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ReviewDto mapToDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setUserId(review.getUser().getId());
        dto.setMovieId(review.getMovie().getId());
        return dto;
    }
}
