package com.example.BookMyShow.service;

import com.example.BookMyShow.dto.*;
import com.example.BookMyShow.exception.ResourceNotFoundException;
import com.example.BookMyShow.exception.SeatUnavailableException;
import com.example.BookMyShow.model.*;
import com.example.BookMyShow.repository.BookingRepository;
import com.example.BookMyShow.repository.ShowRepository;
import com.example.BookMyShow.repository.ShowSeatRepository;
import com.example.BookMyShow.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JavaMailSender mailSender;

    private final WebClient webClient;

    public BookingService(@Value("${nvidia.api.key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://integrate.api.nvidia.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // ── NEW: STEP 1 — Create simulated order + Nvidia AI message ──
    public Map<String, Object> createSimulatedOrder(BookingRequestDto bookingRequest) {

        // Calculate total amount
        List<ShowSeat> seats = showSeatRepository.findAllById(bookingRequest.getSeatIds());
        Double totalAmount = seats.stream().mapToDouble(ShowSeat::getPrice).sum();

        // Get user and show details
        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        Show show = showRepository.findById(bookingRequest.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));

        String seatNumbers = seats.stream()
                .map(s -> s.getSeat().getSeatNumber())
                .collect(Collectors.joining(", "));

        // Generate order ID
        String orderId = "ORD-" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        // Call Nvidia AI to generate confirmation message
        String prompt = String.format(
                "Generate a short friendly movie ticket booking confirmation message for %s " +
                        "who just booked %d seat(s) (%s) for the movie '%s' at %s theater " +
                        "for Rs %.0f. Order ID: %s. Keep it under 3 sentences. Be warm and excited.",
                user.getName(),
                seats.size(),
                seatNumbers,
                show.getMovie().getTitle(),
                show.getScreen().getTheater().getName(),
                totalAmount,
                orderId
        );

        String aiMessage = callNvidiaAI(prompt);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("amount", totalAmount);
        response.put("aiMessage", aiMessage);
        response.put("userName", user.getName());
        response.put("userEmail", user.getEmail());
        response.put("userPhone", user.getPhoneNumber());
        response.put("movieTitle", show.getMovie().getTitle());
        response.put("theaterName", show.getScreen().getTheater().getName());
        response.put("seatNumbers", seatNumbers);

        return response;
    }

    // ── NEW: STEP 2 — Confirm booking + send email ──
    @Transactional
    public BookingDto confirmBooking(BookingRequestDto bookingRequest, String orderId) {

        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

        Show show = showRepository.findById(bookingRequest.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));

        List<ShowSeat> selectedSeats = showSeatRepository
                .findAllById(bookingRequest.getSeatIds());

        // Check availability
        for (ShowSeat seat : selectedSeats) {
            if (!"AVAILABLE".equals(seat.getStatus())) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeat().getSeatNumber() + " is not available");
            }
            seat.setStatus("LOCKED");
        }
        showSeatRepository.saveAll(selectedSeats);

        Double totalAmount = selectedSeats.stream()
                .mapToDouble(ShowSeat::getPrice).sum();

        // Payment
        Payment payment = new Payment();
        payment.setAmount(totalAmount);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentMethod("ONLINE");
        payment.setStatus("SUCCESS");
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setOrderId(orderId);

        // Booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(totalAmount);
        booking.setBookingNumber(UUID.randomUUID().toString());
        booking.setPayment(payment);

        Booking savedBooking = bookingRepository.save(booking);

        selectedSeats.forEach(seat -> {
            seat.setStatus("BOOKED");
            seat.setBooking(savedBooking);
        });
        showSeatRepository.saveAll(selectedSeats);

        // Send email with AI message
        sendConfirmationEmail(user, savedBooking, show, selectedSeats, orderId);

        return mapToBookingDto(savedBooking, selectedSeats);
    }

    // ── Nvidia AI call ──
    private String callNvidiaAI(String prompt) {
        String requestBody = "{"
                + "\"model\": \"meta/llama-3.1-8b-instruct\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \""
                + prompt.replace("\"", "\\\"") + "\"}],"
                + "\"max_tokens\": 150"
                + "}";
        try {
            String aiResponse = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return extractContent(aiResponse);
        } catch (Exception e) {
            System.out.println("Nvidia AI failed: " + e.getMessage());
            return "Your booking is confirmed! Enjoy your movie!";
        }
    }

    // ── Extract content from Nvidia response ──
    private String extractContent(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", "\n");
        } catch (Exception e) {
            return "Your booking is confirmed! Enjoy your movie!";
        }
    }

    // ── Send email ──
    private void sendConfirmationEmail(User user, Booking booking,
                                       Show show, List<ShowSeat> seats,
                                       String orderId) {
        try {
            String seatNumbers = seats.stream()
                    .map(s -> s.getSeat().getSeatNumber())
                    .collect(Collectors.joining(", "));

            // Call Nvidia AI for email message
            String prompt = String.format(
                    "Write a warm booking confirmation email body for %s " +
                            "who booked seats %s for movie '%s' at %s. " +
                            "Amount paid: Rs %.0f. Booking number: %s. Order ID: %s. " +
                            "Keep it friendly and under 5 sentences.",
                    user.getName(), seatNumbers,
                    show.getMovie().getTitle(),
                    show.getScreen().getTheater().getName(),
                    booking.getTotalAmount(),
                    booking.getBookingNumber(),
                    orderId
            );

            String aiMessage = callNvidiaAI(prompt);

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(user.getEmail());
            mail.setSubject("🎬 Booking Confirmed - " + show.getMovie().getTitle());
            mail.setText(
                    "Hi " + user.getName() + ",\n\n" +
                            aiMessage + "\n\n" +
                            "─────────────────────────\n" +
                            "Movie      : " + show.getMovie().getTitle() + "\n" +
                            "Theater    : " + show.getScreen().getTheater().getName() + "\n" +
                            "Show Time  : " + show.getStartTime() + "\n" +
                            "Seats      : " + seatNumbers + "\n" +
                            "Amount     : Rs. " + booking.getTotalAmount() + "\n" +
                            "Booking #  : " + booking.getBookingNumber() + "\n" +
                            "Order ID   : " + orderId + "\n" +
                            "─────────────────────────\n\n" +
                            "Enjoy your movie!\n" +
                            "Team MovieTicketBooking"
            );
            mailSender.send(mail);
            System.out.println("Email sent to: " + user.getEmail());
        } catch (Exception e) {
            System.out.println("Email failed: " + e.getMessage());
        }
    }

    // ── EXISTING methods (unchanged) ─────────────────────

    @Transactional
    public BookingDto createBooking(BookingRequestDto bookingRequest) {
        User user = userRepository.findById(bookingRequest.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));
        Show show = showRepository.findById(bookingRequest.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show Not Found"));
        List<ShowSeat> selectedSeats = showSeatRepository
                .findAllById(bookingRequest.getSeatIds());
        for (ShowSeat seat : selectedSeats) {
            if (!"AVAILABLE".equals(seat.getStatus())) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeat().getSeatNumber() + " is not available");
            }
            seat.setStatus("LOCKED");
        }
        showSeatRepository.saveAll(selectedSeats);
        Double totalAmount = selectedSeats.stream()
                .mapToDouble(ShowSeat::getPrice).sum();
        Payment payment = new Payment();
        payment.setAmount(totalAmount);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentMethod(bookingRequest.getPaymentMethod());
        payment.setStatus("SUCCESS");
        payment.setTransactionId(UUID.randomUUID().toString());
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShow(show);
        booking.setBookingTime(LocalDateTime.now());
        booking.setStatus("CONFIRMED");
        booking.setTotalAmount(totalAmount);
        booking.setBookingNumber(UUID.randomUUID().toString());
        booking.setPayment(payment);
        Booking saveBooking = bookingRepository.save(booking);
        selectedSeats.forEach(seat -> {
            seat.setStatus("BOOKED");
            seat.setBooking(saveBooking);
        });
        showSeatRepository.saveAll(selectedSeats);
        return mapToBookingDto(saveBooking, selectedSeats);
    }

    public BookingDto getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found"));
        List<ShowSeat> seats = showSeatRepository.findAll().stream()
                .filter(seat -> seat.getBooking() != null &&
                        seat.getBooking().getId().equals(booking.getId()))
                .collect(Collectors.toList());
        return mapToBookingDto(booking, seats);
    }

    public BookingDto getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not Found"));
        List<ShowSeat> seats = showSeatRepository.findAll().stream()
                .filter(seat -> seat.getBooking() != null &&
                        seat.getBooking().getId().equals(booking.getId()))
                .collect(Collectors.toList());
        return mapToBookingDto(booking, seats);
    }

    public List<BookingDto> getBookingByUserId(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        return bookings.stream()
                .map(booking -> {
                    List<ShowSeat> seats = showSeatRepository.findAll().stream()
                            .filter(seat -> seat.getBooking() != null &&
                                    seat.getBooking().getId().equals(booking.getId()))
                            .collect(Collectors.toList());
                    return mapToBookingDto(booking, seats);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking Not found"));
        booking.setStatus("CANCELLED");
        List<ShowSeat> seats = showSeatRepository.findAll().stream()
                .filter(seat -> seat.getBooking() != null &&
                        seat.getBooking().getId().equals(booking.getId()))
                .collect(Collectors.toList());
        seats.forEach(seat -> {
            seat.setStatus("AVAILABLE");
            seat.setBooking(null);
        });
        if (booking.getPayment() != null) {
            booking.getPayment().setStatus("REFUNDED");
        }
        Booking updateBooking = bookingRepository.save(booking);
        showSeatRepository.saveAll(seats);
        return mapToBookingDto(updateBooking, seats);
    }

    public BookingDto mapToBookingDto(Booking booking, List<ShowSeat> seats) {
        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(booking.getId());
        bookingDto.setBookingNumber(booking.getBookingNumber());
        bookingDto.setBookingTime(booking.getBookingTime());
        bookingDto.setStatus(booking.getStatus());
        bookingDto.setTotalAmount(booking.getTotalAmount());
        UserDto userDto = new UserDto();
        userDto.setId(booking.getUser().getId());
        userDto.setName(booking.getUser().getName());
        userDto.setEmail(booking.getUser().getEmail());
        userDto.setPhoneNumber(booking.getUser().getPhoneNumber());
        bookingDto.setUser(userDto);
        ShowDto showDto = new ShowDto();
        showDto.setId(booking.getShow().getId());
        showDto.setStartTime(booking.getShow().getStartTime());
        showDto.setEndTime(booking.getShow().getEndTime());
        MovieDto movieDto = new MovieDto();
        movieDto.setId(booking.getShow().getMovie().getId());
        movieDto.setTitle(booking.getShow().getMovie().getTitle());
        movieDto.setDescription(booking.getShow().getMovie().getDescription());
        movieDto.setLanguage(booking.getShow().getMovie().getLanguage());
        movieDto.setGenre(booking.getShow().getMovie().getGenre());
        movieDto.setDurationMins(booking.getShow().getMovie().getDurationMins());
        movieDto.setReleaseDate(booking.getShow().getMovie().getReleaseDate());
        movieDto.setPosterUrl(booking.getShow().getMovie().getPosterUrl());
        showDto.setMovie(movieDto);
        ScreenDto screenDto = new ScreenDto();
        screenDto.setId(booking.getShow().getScreen().getId());
        screenDto.setName(booking.getShow().getScreen().getName());
        screenDto.setTotalSeats(booking.getShow().getScreen().getTotalSeats());
        TheaterDto theaterDto = new TheaterDto();
        theaterDto.setId(booking.getShow().getScreen().getTheater().getId());
        theaterDto.setName(booking.getShow().getScreen().getTheater().getName());
        theaterDto.setAddress(booking.getShow().getScreen().getTheater().getAddress());
        theaterDto.setCity(booking.getShow().getScreen().getTheater().getCity());
        theaterDto.setTotalScreens(booking.getShow().getScreen().getTheater().getTotalScreens());
        screenDto.setTheater(theaterDto);
        showDto.setScreen(screenDto);
        bookingDto.setShow(showDto);
        List<ShowSeatDto> seatDtos = seats.stream()
                .map(seat -> {
                    ShowSeatDto seatDto = new ShowSeatDto();
                    seatDto.setId(seat.getId());
                    seatDto.setStatus(seat.getStatus());
                    seatDto.setPrice(seat.getPrice());
                    SeatDto baseSeatDto = new SeatDto();
                    baseSeatDto.setId(seat.getSeat().getId());
                    baseSeatDto.setSeatNumber(seat.getSeat().getSeatNumber());
                    baseSeatDto.setSeatType(seat.getSeat().getSeatType());
                    baseSeatDto.setBasePrice(seat.getSeat().getBasePrice());
                    seatDto.setSeat(baseSeatDto);
                    return seatDto;
                })
                .collect(Collectors.toList());
        bookingDto.setSeats(seatDtos);
        if (booking.getPayment() != null) {
            PaymentDto paymentDto = new PaymentDto();
            paymentDto.setId(booking.getPayment().getId());
            paymentDto.setAmount(booking.getPayment().getAmount());
            paymentDto.setPaymentMethod(booking.getPayment().getPaymentMethod());
            paymentDto.setPaymentTime(booking.getPayment().getPaymentTime());
            paymentDto.setStatus(booking.getPayment().getStatus());
            paymentDto.setTransactionId(booking.getPayment().getTransactionId());
            bookingDto.setPayment(paymentDto);
        }
        return bookingDto;
    }
}