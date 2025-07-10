package ru.college.carmarketplace.service.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.college.carmarketplace.model.FavoriteRequest;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.model.dtos.CarImageDTO;
import ru.college.carmarketplace.model.dtos.CarParameterDTO;
import ru.college.carmarketplace.model.dtos.FavoriteDTO;
import ru.college.carmarketplace.model.entities.AppUser;
import ru.college.carmarketplace.model.entities.Car;
import ru.college.carmarketplace.model.entities.CarImage;
import ru.college.carmarketplace.model.entities.Favorites;
import ru.college.carmarketplace.repo.CarRepository;
import ru.college.carmarketplace.repo.FavoritesRepository;
import ru.college.carmarketplace.repo.TokenRepository;
import ru.college.carmarketplace.repo.UserRepository;
import ru.college.carmarketplace.service.FavoriteService;
import ru.college.carmarketplace.service.JwtService;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {
    private final CarRepository carRepository;
    private final FavoritesRepository favoritesRepository;
    private final TokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    @Override
    public String addToFavorites(FavoriteRequest favoriteRequest, HttpServletRequest request) {
        try {
            // забираю токен
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header");
            }

            String token = authHeader.substring(7);


            Integer userId = jwtService.extractUserId(token);

            if(favoritesRepository.existsByUserIdAndCarId(userId, favoriteRequest.getId())){
                return "Машина уже в избранном";
            }

            Car car = getProduct(favoriteRequest.getId());

            Favorites favorites = new Favorites();
            favorites.setUserId(userId);
            favorites.setCar(car);
            favoritesRepository.save(favorites);

            return "Успешно добавлено";
        } catch (EntityNotFoundException e) {
            throw new EntityNotFoundException(e.getMessage());
        }
    }

    @Override
    public String removeFromFavorites(FavoriteRequest favoriteRequest, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный заголовок авторизации");
        }

        String token = authHeader.substring(7);
        Integer userId = jwtService.extractUserId(token);
        Long carId = favoriteRequest.getId();

        if (!carRepository.existsById(carId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Автомобиль с ID " + carId + " не найден");
        }
        Optional<Favorites> favoriteOpt = favoritesRepository.findByUserIdAndCarId(userId, carId);

        if (favoriteOpt.isEmpty()) {
            return "Автомобиль не был в избранном";
        }

        favoritesRepository.delete(favoriteOpt.get());
        return "Автомобиль успешно удален из избранного";
    }

    @Override
    public List<CarDTO> getUserFavoriteCars() {
        return favoritesRepository.findAll().stream()
                .filter(favorite -> favorite.getCar() != null)
                .map(favorite -> {
                    Car car = favorite.getCar();
                    CarDTO dto = new CarDTO();
                    dto.setId(car.getId());
                    dto.setBrand(car.getBrand());
                    dto.setModel(car.getModel());
                    dto.setPrice(car.getPrice());
                    dto.setEngineType(car.getEngineType());
                    dto.setTransmission(car.getTransmission());
                    dto.setDriveType(car.getDriveType());
                    dto.setCapacity(car.getCapacity());
                    dto.setBodyType(car.getBodyType());
                    dto.setYear(car.getYear());
                    dto.setEngineVolume(car.getEngineVolume());
                    dto.setMileage(car.getMileage());
                    dto.setLocation(car.getLocation());
                    dto.setTitle(car.getTitle());
                    dto.setExclusives(car.getExclusives());
                    dto.setRecommends(car.getRecommends());

                    // Группируем изображения по типам
                    if (car.getImageUrl() != null) {
                        dto.setImageUrl(car.getImageUrl().stream()
                                .collect(Collectors.groupingBy(
                                        CarImage::getImageType,
                                        Collectors.mapping(CarImage::getImageUrl, Collectors.toList())
                                )));
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public Car getProduct(Long id) {
        return carRepository.findById(id).orElse(null);
    }
}
