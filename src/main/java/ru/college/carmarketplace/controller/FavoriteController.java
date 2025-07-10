package ru.college.carmarketplace.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.college.carmarketplace.model.FavoriteRequest;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.model.dtos.FavoriteDTO;
import ru.college.carmarketplace.model.entities.Car;
import ru.college.carmarketplace.model.entities.Favorites;
import ru.college.carmarketplace.service.FavoriteService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<List<CarDTO>> getAllFavorites() {
        return ResponseEntity.ok(favoriteService.getUserFavoriteCars());
    }

    @PostMapping("/add")
    public ResponseEntity<String> addFavorite(@RequestBody FavoriteRequest favoriteRequest, HttpServletRequest request) {
        return ResponseEntity.ok(favoriteService.addToFavorites(favoriteRequest, request));
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteFavorite(@RequestBody FavoriteRequest favoriteRequest, HttpServletRequest request) {
        return ResponseEntity.ok(favoriteService.removeFromFavorites(favoriteRequest, request));
    }
}
