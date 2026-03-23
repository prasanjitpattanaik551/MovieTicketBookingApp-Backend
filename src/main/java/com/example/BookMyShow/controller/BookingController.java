package com.example.BookMyShow.controller;

import com.example.BookMyShow.dto.BookingDto;
import com.example.BookMyShow.dto.BookingRequestDto;
import com.example.BookMyShow.service.BookingService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody BookingRequestDto bookingRequest)
    {
        return new ResponseEntity<>(bookingService.createBooking(bookingRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingDto> getBookingById(@PathVariable Long id)
    {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    // Get booking by booking number
    @GetMapping("/number/{bookingNumber}")
    public ResponseEntity<BookingDto> getBookingByNumber(@PathVariable String bookingNumber)
    {
        return ResponseEntity.ok(bookingService.getBookingByNumber(bookingNumber));
    }

    // Get all bookings of a user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingDto>> getBookingsByUser(@PathVariable Long userId)
    {
        return ResponseEntity.ok(bookingService.getBookingByUserId(userId));
    }

    // Cancel booking
    @PutMapping("/cancel/{id}")
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable Long id)
    {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}