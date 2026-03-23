package com.example.BookMyShow.controller;

import com.example.BookMyShow.dto.ShowDto;
import com.example.BookMyShow.dto.ShowSeatDto;
import com.example.BookMyShow.service.ShowService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    @PostMapping
    public ResponseEntity<ShowDto> createShow(@RequestBody ShowDto showDto) {
        return ResponseEntity.ok(showService.creteShow(showDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowDto> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }

    @GetMapping
    public ResponseEntity<List<ShowDto>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowDto>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    @GetMapping("/movie/{movieId}/city/{city}")
    public ResponseEntity<List<ShowDto>> getShowsByMovieAndCity(@PathVariable Long movieId,
                                                                @PathVariable String city) {
        return ResponseEntity.ok(showService.getShowsByMovieAndCity(movieId, city));
    }

    @GetMapping("/range")
    public ResponseEntity<List<ShowDto>> getShowsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        return ResponseEntity.ok(showService.getShowsByDateRange(start, end));
    }


    @GetMapping("/{id}/seats")
    public ResponseEntity<List<ShowSeatDto>> getAllSeatsForShow(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getAllSeatsForShow(id));
    }
}