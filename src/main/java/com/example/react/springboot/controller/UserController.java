package com.example.react.springboot.controller;

import com.example.react.springboot.exception.UserNotFoundException;
import com.example.react.springboot.model.User;
import com.example.react.springboot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@SpringBootApplication
@Configuration
@EnableWebMvc
@CrossOrigin("http://localhost:3000")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/user")
    public User newUser(@RequestParam("name") String name,
                        @RequestParam("username") String username,
                        @RequestParam("email") String email,
                        @RequestParam("phone") String phone,
                        @RequestParam("address") String address,
                        @RequestParam("image") MultipartFile file) throws IOException {

        // Save file to the server
        String fileName = file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, file.getBytes());
        String imagePath = filePath.toString().replace("\\", "/");

        // Create User object and save to the database
        User newUser = new User();
        newUser.setName(name);
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setAddress(address);
        newUser.setImage(imagePath);  // Save file path

        return userRepository.save(newUser);
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/user/{id}")
    public User getUserById(@PathVariable Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    @PutMapping("/user/{id}")
    public User updateUser(@RequestParam("name") String name,
                           @RequestParam("username") String username,
                           @RequestParam("email") String email,
                           @RequestParam("phone") String phone,
                           @RequestParam("address") String address,
                           @RequestParam(value = "image", required = false) MultipartFile file,
                           @PathVariable Long id) throws IOException {

        return userRepository.findById(id).map(user -> {
            user.setName(name);
            user.setUsername(username);
            user.setEmail(email);
            user.setPhone(phone);
            user.setAddress(address);

            if (file != null && !file.isEmpty()) {
                // Kiểm tra nếu tệp đã tồn tại
                String fileName = file.getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                if (!Files.exists(filePath)) {
                    try {
                        Files.copy(file.getInputStream(), filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to save new image", e);
                    }
                }

                String imagePath = filePath.toString().replace("\\", "/");

                // Cập nhật đường dẫn hình ảnh
                user.setImage(imagePath);
            }

            return userRepository.save(user);
        }).orElseThrow(() -> new UserNotFoundException(id));
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        userRepository.deleteById(id);
        return ResponseEntity.status(HttpStatus.OK).body("User with ID " + id + " has been deleted successfully.");
    }
}