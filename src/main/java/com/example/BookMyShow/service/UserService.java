package com.example.BookMyShow.service;

import com.example.BookMyShow.dto.LoginRequestDto;
import com.example.BookMyShow.model.User;
import com.example.BookMyShow.dto.UserDto;
import com.example.BookMyShow.exception.ResourceNotFoundException;
import com.example.BookMyShow.repository.UserRepository;
import com.example.BookMyShow.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;




    @Autowired
    private JavaMailSender mailSender;

    // STEP 1 — Generate OTP and send email
    public String sendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Generate 6-digit OTP
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Save OTP with 5 minute expiry
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Send OTP email
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom("YOUR_OUTLOOK_EMAIL@outlook.com");
        mail.setTo(email);
        mail.setSubject("Your OTP - MovieTicketBooking");
        mail.setText(
                "Hi " + user.getName() + ",\n\n" +
                        "Your OTP for login is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n\n" +
                        "Team MovieTicketBooking"
        );
        mailSender.send(mail);

        return "OTP sent to " + email;
    }

    // STEP 2 — Verify OTP and return JWT token
    public String verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check OTP
        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        // Check expiry
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new RuntimeException("OTP expired");
        }

        // Clear OTP after successful verify
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        // Return JWT token
        return jwtTokenProvider.generateToken(email);
    }


    public UserDto createUser(UserDto userDto)
    {
        User user=mapToEntity(userDto);
        User savedUser=userRepository.save(user);
        return mapToDto(savedUser);
    }

    public UserDto getUserById(Long id)
    {
        User user=userRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Use not found with id: "+id));
        return mapToDto(user);
    }

    public List<UserDto> getAllUsers()
    {
        List<User> users=userRepository.findAll();
        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    //update user
    //delete user




    public String login(LoginRequestDto loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        // Generate and return JWT token
        return jwtTokenProvider.generateToken(loginRequest.getEmail());
    }

    public User mapToEntity(UserDto userDto) {

        User user = new User();

        user.setEmail(userDto.getEmail());
        user.setName(userDto.getName());
        user.setPhoneNumber(userDto.getPhoneNumber());

        // encode password before saving
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        return user;
    }

    public UserDto mapToDto(User user)
    {
        UserDto userDto=new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setPhoneNumber(user.getPhoneNumber());
        return userDto;

    }


    public UserDto updateUser(Long id, UserDto userDto)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        user.setPhoneNumber(userDto.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        return mapToDto(updatedUser);
    }


    public void deleteUser(Long id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
    }


}
