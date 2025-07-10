package ru.college.carmarketplace.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.college.carmarketplace.model.FavoriteRequest;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.model.dtos.FavoriteDTO;
import ru.college.carmarketplace.model.entities.Favorites;

import java.util.List;

public interface FavoriteService {
    String addToFavorites(FavoriteRequest favoriteRequest, HttpServletRequest request);
    String removeFromFavorites(FavoriteRequest favoriteRequest, HttpServletRequest request);
    List<CarDTO> getUserFavoriteCars();
}
