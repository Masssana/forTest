package ru.college.carmarketplace.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import ru.college.carmarketplace.model.PhoneNumberRequest;
import ru.college.carmarketplace.model.UserUpdateRequest;

public interface UserService {
    void updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request);
    ResponseEntity<String> isAccessExpired();
    ResponseEntity<String> setPhoneNumber(PhoneNumberRequest numberRequest, HttpServletRequest request);
}
