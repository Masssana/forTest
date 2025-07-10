package ru.college.carmarketplace.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.college.carmarketplace.model.dtos.CarFilter;
import ru.college.carmarketplace.repo.SvgImagesRepository;
import ru.college.carmarketplace.service.CarSpecificationBuilder;
import ru.college.carmarketplace.model.Suggestions;
import ru.college.carmarketplace.model.ValueSuggestion;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.repo.CarRepository;
import ru.college.carmarketplace.service.ProductService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final CarRepository carRepository;
    private final CarSpecificationBuilder specificationBuilder;
    private final SvgImagesRepository imagesRepository;
    private List<String> brands = new ArrayList<>();

    private int ENGINE_VOLUME = 10;
    private int PRICE = 10000000;
    private int MILEAGE = 10000000;
    private int YEAR = 2025;

    public CarDTO getProduct(Long id) {
        return carRepository.findById(id).map(CarDTO::getDto).orElse(null);
    }

    @Override
    public List<String> getBrands() {
        brands = carRepository.findAllBrands();
        return brands;
    }


    @Override
    public List<Suggestions> searchSuggestions(String query) {
        List<Suggestions> suggestions = new ArrayList<>();
        brands.stream()
                .filter(brand -> brand.toLowerCase()
                        .contains(query.toLowerCase()))
                .limit(3).
                forEach(brand -> {
                    suggestions.add(new Suggestions(
                            "https://90.156.154.121:9000/images/images.png",
                            "brand",
                            new ValueSuggestion(brand, null, null)));
                });

        carRepository.findTop3ByModelContainingIgnoreCase(query)
                .stream()
                .limit(2)
                .forEach(car ->{
                    suggestions.add(new Suggestions(
                            "so?.jpg",
                            "model",
                            new ValueSuggestion(car.getBrand(), car.getModel(), null)
                    ));
                });

        suggestions.add(new Suggestions(
                "https://90.156.154.121:9000/images/poisk.png",
                "text",
                new ValueSuggestion(null, null, query.trim())
        ));

        return suggestions;
    }

    @Override
    public Map<String, Object> getFilterParameters(String[] brands) {
        Map<String, Object> filters = new HashMap<>();
        List<String> allBrands = carRepository.findAllBrands();
        filters.put("brands", allBrands);

        if(brands != null && brands.length > 0) {
            List<String> models = new ArrayList<>();
            for (String brand : brands) {
                if (brand != null && !brand.isEmpty()) {
                    models.addAll(carRepository.findModelsByBrand(brand));
                }
            }
            filters.put("models", models);
        }

        // Остальной код остается без изменений
        filters.put("transmission", Arrays.asList("Механика", "Автомат", "Робот", "Вариатор"));
        filters.put("engineType", Arrays.asList("Бензин", "Дизель", "Электро", "Гибрид", "ГБО"));
        filters.put("driveType", Arrays.asList("Передний", "Задний", "Полный"));
        filters.put("capacity", Arrays.asList("2 места", "5 мест", "7 мест", "9 мест", "11 мест"));
        filters.put("bodyType", Arrays.asList(
                "Седан", "Купе", "Кроссовер", "Внедорожник",
                "Хэтчбек", "Лифтбек", "Универсал", "Фастбэк",
                "Пикап", "Минивэн", "Кабриолет", "Лимузин"
        ));
        filters.put("engineVolume", Map.of("min", 0, "max", ENGINE_VOLUME));
        filters.put("mileage", Map.of("min", 0, "max", MILEAGE));
        filters.put("year", Map.of("min", 0, "max", YEAR));
        filters.put("price", Map.of("min", 0, "max", PRICE));

        return filters;
    }

    public Page<CarDTO> getCarsByParams(CarFilter filter, Pageable pageable){
        var query = specificationBuilder.build(filter);

        query = query.and(((root, query1, cb) -> cb.equal(root.get("booked"), false)));
        return carRepository.findAll(query, pageable)
                .map(CarDTO::getDto);
    }

    public String getSvgImage(String name){
        return imagesRepository.findDataByName(name).getData();
    }

}
