package ru.college.carmarketplace.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.server.ResponseStatusException;
import ru.college.carmarketplace.enums.Role;
import ru.college.carmarketplace.exception.CustomException;
import ru.college.carmarketplace.exception.ValidationException;
import ru.college.carmarketplace.model.ErrorResponse;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.model.dtos.CarParameterDTO;
import ru.college.carmarketplace.model.dtos.OrderDTO;
import ru.college.carmarketplace.model.entities.*;
import ru.college.carmarketplace.repo.CarRepository;
import ru.college.carmarketplace.repo.OrderRepository;
import ru.college.carmarketplace.repo.UserRepository;
import ru.college.carmarketplace.service.AdminService;
import ru.college.carmarketplace.service.JwtService;
import ru.college.carmarketplace.service.MinioService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final CarRepository carRepository;
    private final OrderRepository orderRepository;
    private final MinioService minioService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<Car> create(CarDTO carDTO, MultipartFile[] mainImages,
                                      MultipartFile[] exclusiveImages, MultipartFile[] recommendImages) throws IOException {
        // Сначала загружаем изображения
        Map<String, List<String>> imagesMap = new HashMap<>();

        imagesMap.put("main", uploadImages(mainImages));
        imagesMap.put("exclusives", uploadImages(exclusiveImages));
        imagesMap.put("recommends", uploadImages(recommendImages));

        carDTO.setImageUrl(imagesMap);
        Car car = convertToProduct(carDTO);

        Car createdCar = carRepository.save(car);
        return new ResponseEntity<>(createdCar, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<?> tryCreatingCar(String title,
                                            String brand,
                                            String model,
                                            String price,
                                            String engineType,
                                            String transmission,
                                            String driveType,
                                            String bodyType,
                                            String year,
                                            String engineVolume,
                                            String capacity,
                                            String mileage,
                                            String exclusives,
                                            String recommends,
                                            String location,
                                            String parametersJson,
                                            MultipartFile[] mainFiles,
                                            MultipartFile[] exclusiveFiles,
                                            MultipartFile[] recommendFiles){
        try {
//            ErrorResponse errorResponse = new ErrorResponse();
//            validateBasicInfo(errorResponse, title, brand, model);
//            validatePriceAndEngineInfo(errorResponse, price, engineType, transmission);
//            validateCarSpecs(errorResponse, driveType, bodyType, year);
//            validateNumericValues(errorResponse, engineVolume, capacity, mileage);
//            validateLocationAndImages(errorResponse, location, mainFiles);
//
//            if(!errorResponse.getErrors().isEmpty()) {
//                throw new ValidationException(errorResponse.getErrors());
//            }

            CarDTO carDTO = buildCarDTOFromJson(
                    title, brand, model, price, engineType, transmission,
                    driveType, bodyType, year, engineVolume, capacity, mileage,
                    exclusives, recommends, location, parametersJson
            );

            create(
                    carDTO,
                    mainFiles,
                    exclusiveFiles,
                    recommendFiles
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (JsonProcessingException e) {

            return ResponseEntity.badRequest().body("Invalid parameters JSON");
        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }

    private void validateBasicInfo(ErrorResponse errorResponse, String title, String brand, String model) {
        if (title == null) {
            errorResponse.addError("title", "Поле 'Название' обязательно для заполнения");
        }
        if (brand == null || brand.trim().isEmpty()) {
            errorResponse.addError("brand", "Поле 'Марка' обязательно для заполнения");
        }
        if (model == null || model.trim().isEmpty()) {
            errorResponse.addError("model", "Поле 'Модель' обязательно для заполнения");
        }
    }

    private void validatePriceAndEngineInfo(ErrorResponse errorResponse, String price, String engineType, String transmission) {
        if (price == null || price.trim().isEmpty()) {
            errorResponse.addError("price", "Поле 'Цена' обязательно для заполнения");
        } else {
            try {
                new BigDecimal(price);
            } catch (NumberFormatException e) {
                errorResponse.addError("price", "Неверный формат цены");
            }
        }
        if (engineType == null || engineType.trim().isEmpty()) {
            errorResponse.addError("engineType", "Поле 'Тип двигателя' обязательно для заполнения");
        }
        if (transmission == null || transmission.trim().isEmpty()) {
            errorResponse.addError("transmission", "Поле 'Коробка передач' обязательно для заполнения");
        }
    }

    private void validateCarSpecs(ErrorResponse errorResponse, String driveType, String bodyType, String year) {
        if (driveType == null || driveType.trim().isEmpty()) {
            errorResponse.addError("driveType", "Поле 'Привод' обязательно для заполнения");
        }
        if (bodyType == null || bodyType.trim().isEmpty()) {
            errorResponse.addError("bodyType", "Поле 'Тип кузова' обязательно для заполнения");
        }
        if (year == null || year.trim().isEmpty()) {
            errorResponse.addError("year", "Поле 'Год выпуска' обязательно для заполнения");
        } else {
            try {
                Integer.parseInt(year);
            } catch (NumberFormatException e) {
                errorResponse.addError("year", "Неверный формат года");
            }
        }
    }

    private void validateNumericValues(ErrorResponse errorResponse, String engineVolume, String capacity, String mileage) {
        if (engineVolume == null || engineVolume.trim().isEmpty()) {
            errorResponse.addError("engineVolume", "Поле 'Объем двигателя' обязательно для заполнения");
        } else {
            try {
                Double.parseDouble(engineVolume);
            } catch (NumberFormatException e) {
                errorResponse.addError("engineVolume", "Неверный формат объема двигателя");
            }
        }
        if (capacity == null || capacity.trim().isEmpty()) {
            errorResponse.addError("capacity", "Поле 'Количество мест' обязательно для заполнения");
        } else {
            try {
                Short.parseShort(capacity);
            } catch (NumberFormatException e) {
                errorResponse.addError("capacity", "Неверный формат количества мест");
            }
        }
        if (mileage == null || mileage.trim().isEmpty()) {
            errorResponse.addError("mileage", "Поле 'Пробег' обязательно для заполнения");
        } else {
            try {
                Integer.parseInt(mileage);
            } catch (NumberFormatException e) {
                errorResponse.addError("mileage", "Неверный формат пробега");
            }
        }
    }

    private void validateLocationAndImages(ErrorResponse errorResponse, String location, MultipartFile[] mainFiles) {
        if (location == null || location.trim().isEmpty()) {
            errorResponse.addError("location", "Поле 'Местоположение' обязательно для заполнения");
        }
        if (mainFiles == null || mainFiles.length == 0) {
            errorResponse.addError("mainImages", "Необходимо загрузить хотя бы одно основное изображение");
        }
    }

    @Override
    public void deleteProduct(Long id) {
        carRepository.deleteById(id);
    }

    @Override
    public void updateCar(Long id, CarDTO carDTO, MultipartFile[] mainFiles,
                          MultipartFile[] exclusiveFiles, MultipartFile[] recommendFiles,
                          List<String> oldMain, List<String> oldExclusives,
                          List<String> oldRecommends) throws IOException {

        Map<String, List<String>> imagesMap = new HashMap<>();

        // Старые изображения
        if (oldMain != null) imagesMap.put("main", oldMain);
        if (oldExclusives != null) imagesMap.put("exclusives", oldExclusives);
        if (oldRecommends != null) imagesMap.put("recommends", oldRecommends);


        // Новые изображения
        addImagesToMap(imagesMap, "main", mainFiles);
        addImagesToMap(imagesMap, "exclusives", exclusiveFiles);
        addImagesToMap(imagesMap, "recommends", recommendFiles);

        carDTO.setImageUrl(imagesMap);
        updateProduct(id, carDTO);
    }

    @Override
    public ResponseEntity<?> tryUpdateCar(Long id,
                                          String title,
                                          String brand,
                                          String model,
                                          String price,
                                          String engineType,
                                          String transmission,
                                          String driveType,
                                          String bodyType,
                                          String year,
                                          String engineVolume,
                                          String capacity,
                                          String mileage,
                                          String exclusives,
                                          String recommends,
                                          String location,
                                          String parametersJson,
                                          String oldMainJson,
                                          String oldExclusivesJson,
                                          String oldRecommendsJson,
                                          MultipartFile[] mainFiles,
                                          MultipartFile[] exclusiveFiles,
                                          MultipartFile[] recommendFiles){
        try {
            CarDTO carDTO = buildCarDTOFromJson(
                    title, brand, model, price, engineType, transmission,
                    driveType, bodyType, year, engineVolume, capacity, mileage,
                    exclusives, recommends, location, parametersJson
            );

            List<String> oldMain = parseJsonList(oldMainJson);
            List<String> oldExclusives = parseJsonList(oldExclusivesJson);
            List<String> oldRecommends = parseJsonList(oldRecommendsJson);

            updateCar(
                    id,
                    carDTO,
                    mainFiles,
                    exclusiveFiles,
                    recommendFiles,
                    oldMain,
                    oldExclusives,
                    oldRecommends
            );
            return ResponseEntity.ok().build();
        } catch (JsonProcessingException e) {

            return ResponseEntity.badRequest().body("Invalid JSON format");
        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }

    private List<String> parseJsonList(String json) throws JsonProcessingException {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    }

    @Override
    public CarDTO getCarById(Long id) {
        return carRepository.findById(id).map(CarDTO::getDto)
                .orElseThrow(() -> new EntityNotFoundException("Car not found"));

    }

    @Override
    public Page<CarDTO> getAllCars(Long search, Pageable pageable) {
        if (search != null) {
            return carRepository.findById(search, pageable).map(CarDTO::getDto);
        }else {
            return carRepository.findAll(pageable)
                    .map(CarDTO::getDto);
        }
    }

    @Override
    public String uploadImage(MultipartFile file) {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        minioService.uploadFile(file, fileName);
        return minioService.getFileUrl(fileName);
    }

    // Вспомогательные методы
    private Car convertToProduct(CarDTO carDTO) {
        Car car = new Car();
        car.setBrand(carDTO.getBrand());
        car.setModel(carDTO.getModel());
        car.setPrice(carDTO.getPrice());
        car.setEngineType(carDTO.getEngineType());
        car.setTransmission(carDTO.getTransmission());
        car.setDriveType(carDTO.getDriveType());
        car.setCapacity(carDTO.getCapacity());
        car.setBodyType(carDTO.getBodyType());
        car.setYear(carDTO.getYear());
        car.setEngineVolume(carDTO.getEngineVolume());
        car.setMileage(carDTO.getMileage());
        car.setLocation(carDTO.getLocation());
        car.setTitle(carDTO.getTitle());
        car.setExclusives(carDTO.getExclusives());
        car.setRecommends(carDTO.getRecommends());
        car.setCreateAt(carDTO.getCreateAt());

        if (carDTO.getImageUrl() != null) {
            List<CarImage> images = new ArrayList<>();
            carDTO.getImageUrl().forEach((type, urls) ->
                    urls.forEach(url ->
                            images.add(new CarImage(null, type, url))
                    ));
            car.setImageUrl(images);
        }
        if(carDTO.getParameters() != null) {
            List<CarParameter> parameters = carDTO.getParameters()
                    .stream()
                    .map(param -> {
                        CarParameter carParameter = new CarParameter();

                        carParameter.setImage(param.getImage());
                        carParameter.setTitle(param.getTitle());
                        carParameter.setAvailable(param.isAvailable());
                        return carParameter;
                    }).collect(Collectors.toList());
            car.setParameters(parameters);

        }



        return car;
    }

    private void updateProduct(Long id, CarDTO carDTO) {
        Car existingCar = carRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Car not found with id: " + id));
        updateCarFromDTO(existingCar, carDTO);
        carRepository.save(existingCar);
    }

    private void updateCarFromDTO(Car car, CarDTO carDTO) {
        car.setBrand(carDTO.getBrand());
        car.setModel(carDTO.getModel());
        car.setPrice(carDTO.getPrice());
        car.setEngineType(carDTO.getEngineType());
        car.setTransmission(carDTO.getTransmission());
        car.setDriveType(carDTO.getDriveType());
        car.setCapacity(carDTO.getCapacity());
        car.setBodyType(carDTO.getBodyType());
        car.setYear(carDTO.getYear());
        car.setEngineVolume(carDTO.getEngineVolume());
        car.setMileage(carDTO.getMileage());
        car.setLocation(carDTO.getLocation());
        car.setTitle(carDTO.getTitle());
        car.setExclusives(carDTO.getExclusives());
        car.setRecommends(carDTO.getRecommends());
        car.setCreateAt(carDTO.getCreateAt());

        updateImages(car, carDTO);
        updateParameters(car, carDTO);


    }

    private void updateImages(Car car, CarDTO carDTO) {
        // Удаляем только те изображения, которых нет в новом DTO
        Set<CarImage> imagesToRemove = new HashSet<>(car.getImageUrl());
        if (carDTO.getImageUrl() != null) {
            carDTO.getImageUrl().forEach((type, urls) ->
                    urls.forEach(url -> {
                        // Проверяем, существует ли уже такое изображение
                        boolean exists = car.getImageUrl().stream()
                                .anyMatch(img -> img.getImageType().equals(type) && img.getImageUrl().equals(url));

                        if (!exists) {
                            car.getImageUrl().add(new CarImage(null, type, url));
                        }

                        // Убираем из списка на удаление
                        imagesToRemove.removeIf(img ->
                                img.getImageType().equals(type) && img.getImageUrl().equals(url));
                    })
            );
        }
        car.getImageUrl().removeAll(imagesToRemove);
    }

    private void updateParameters(Car car, CarDTO carDTO) {
        if (carDTO.getParameters() == null) {
            car.getParameters().clear();
            return;
        }


        Map<Long, CarParameterDTO> newParamsMap = carDTO.getParameters().stream()
                .filter(param -> param.getId() != null)
                .collect(Collectors.toMap(CarParameterDTO::getId, Function.identity()));

        Iterator<CarParameter> iterator = car.getParameters().iterator();
        while (iterator.hasNext()) {
            CarParameter existingParam = iterator.next();
            CarParameterDTO newParam = newParamsMap.get(existingParam.getId());

            if (newParam != null) {

                existingParam.setImage(newParam.getImage());
                existingParam.setTitle(newParam.getTitle());
                existingParam.setAvailable(newParam.isAvailable());
                newParamsMap.remove(existingParam.getId());
            } else {
                iterator.remove();
            }
        }

        newParamsMap.values().forEach(param -> {
            CarParameter newCarParam = new CarParameter();
            newCarParam.setImage(param.getImage());
            newCarParam.setTitle(param.getTitle());
            newCarParam.setAvailable(param.isAvailable());
            car.getParameters().add(newCarParam);
        });
    }

    private List<String> uploadImages(MultipartFile[] files) throws IOException {
        if (files == null) return Collections.emptyList();

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                urls.add(uploadImage(file));
            }
        }
        return urls;
    }

    private void addImagesToMap(Map<String, List<String>> map, String type,
                                MultipartFile[] files) throws IOException {
        if (files != null && files.length > 0) {
            List<String> urls = map.getOrDefault(type, new ArrayList<>());
            urls.addAll(uploadImages(files));
            map.put(type, urls);
        }
    }

    @Override
    public List<OrderDTO> getNewOrders(String search){
        if(search == null || search.trim().isEmpty()){
            return orderRepository.findAllCreated()
                    .stream()
                    .map(OrderDTO::toDTO)
                    .collect(Collectors.toList());
        }

        return orderRepository.findCreatedOrdersBySearch(search)
                .stream()
                .map(OrderDTO::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getReadyOrders(String search) {
        if(search == null || search.trim().isEmpty()){
            return orderRepository.findAllByStatus()
                    .stream()
                    .map(OrderDTO::toDTO).collect(Collectors.toList());
        }

        return orderRepository.findAllByStatusAndSearch(search.trim())
                .stream()
                .map(OrderDTO::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getNotReadyOrders(String search) {
        if(search == null || search.trim().isEmpty()){
            return orderRepository.findCancelledAndReceivedOrders()
                    .stream().map(OrderDTO::toDTO)
                    .collect(Collectors.toList());
        }

        return orderRepository.findCancelledAndReceivedOrdersBySearch(search.trim())
                .stream()
                .map(OrderDTO::toDTO)
                .collect(Collectors.toList());
    }

    private CarDTO buildCarDTOFromJson(
            String title, String brand, String model, String price, String engineType,
            String transmission, String driveType, String bodyType, String year,
            String engineVolume,
            String capacity, String mileage, String exclusives, String recommends,
            String location, String parametersJson
    ) throws JsonProcessingException {
        CarDTO carDTO = new CarDTO();
        carDTO.setTitle(title);
        carDTO.setBrand(brand);
        carDTO.setModel(model);
        carDTO.setPrice(new BigDecimal(price));
        carDTO.setEngineType(engineType);
        carDTO.setTransmission(transmission);
        carDTO.setDriveType(driveType);
        carDTO.setBodyType(bodyType);
        carDTO.setYear(Integer.parseInt(year));
        carDTO.setEngineVolume(Double.parseDouble(engineVolume));
        carDTO.setCapacity(Short.parseShort(capacity));
        carDTO.setMileage(Integer.parseInt(mileage));
        carDTO.setLocation(location);
        carDTO.setExclusives(exclusives);
        carDTO.setRecommends(recommends);

        List<CarParameterDTO> parameters = objectMapper.readValue(
                parametersJson,
                new TypeReference<List<CarParameterDTO>>() {}
        );
        carDTO.setParameters(parameters);

        return carDTO;
    }

    public ResponseEntity<Map<String, Object>> getListingOptions() {
        Map<String, Object> options = new HashMap<>();

        options.put("transmission", Arrays.asList(
                createOption("Механика", "Механика"),
                createOption("Автомат", "Автомат"),
                createOption("Робот", "Робот"),
                createOption("Вариатор", "Вариатор")
        ));

        options.put("engineType", Arrays.asList(
                createOption("Бензин", "Бензин"),
                createOption("Дизель", "Дизель"),
                createOption("Электро", "Электро"),
                createOption("Гибрид", "Гибрид"),
                createOption("ГБО", "ГБО")
        ));

        options.put("driveType", Arrays.asList(
                createOption("Передний", "Передний"),
                createOption("Задний", "Задний"),
                createOption("Полный", "Полный")
        ));

        options.put("capacity", Arrays.asList(
                createOption("2 места", 2),
                createOption("4 места", 4),
                createOption("5 мест", 5),
                createOption("7 мест", 7),
                createOption("9 мест", 9),
                createOption("11 мест", 11)
        ));

        options.put("bodyType", Arrays.asList(
                createOption("Седан", "Седан"),
                createOption("Купе", "Купе"),
                createOption("Кроссовер", "Кроссовер"),
                createOption("Внедорожник", "Внедорожник"),
                createOption("Хэтчбек", "Хэтчбек"),
                createOption("Лифтбек", "Лифтбек"),
                createOption("Универсал", "Универсал"),
                createOption("Фастбэк", "Фастбэк"),
                createOption("Пикап", "Пикап"),
                createOption("Минивэн", "Минивэн"),
                createOption("Кабриолет", "Кабриолет"),
                createOption("Лимузин", "Лимузин")
        ));

        List<String> svgIcons = Arrays.asList(
                "sits", "vent", "hot", "navigation", "key",
                "cond", "camera", "parking", "led", "roof"
        );

        List<Map<String, Object>> parameters = new ArrayList<>();
        for (int i = 0; i < svgIcons.size(); i++) {
            Map<String, Object> param = new HashMap<>();
            param.put("id", i + 1);
            param.put("image", svgIcons.get(i));
            param.put("title", getParameterTitle(svgIcons.get(i)));
            param.put("available", false);
            parameters.add(param);
        }
        options.put("parameters", parameters);

        return ResponseEntity.ok(options);
    }

    private String getParameterTitle(String svgName) {
        return switch (svgName) {
            case "sits" -> "Люк на крыше";
            case "vent" -> "Фара(LED)";
            case "hot" -> "Датчик Парковки";
            case "navigation" -> "Задняя камера";
            case "key" -> "Автоматический Кондиционер";
            case "cond" -> "Смарт Ключ";
            case "camera" -> "Навигация";
            case "parking" -> "Подогрев Сидений";
            case "led" -> "Вентиляционный лист";
            case "roof" -> "Кожаные сидения";
            default -> "Параметр " + svgName;
        };
    }

    private Map<String, Object> createOption(String label, Object value) {
        Map<String, Object> option = new HashMap<>();
        option.put("label", label);
        option.put("value", value);
        return option;
    }

}