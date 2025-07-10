package ru.college.carmarketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.college.carmarketplace.annotations.AdminOnly;
import ru.college.carmarketplace.model.dtos.*;
import ru.college.carmarketplace.service.AdminService;
import ru.college.carmarketplace.model.entities.Car;
import ru.college.carmarketplace.service.ProductService;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/listing")
public class AdminController {
    private final AdminService adminService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @PostMapping("/create")
    @AdminOnly
    public ResponseEntity<Car> createProduct(@ModelAttribute CarDTO product, @RequestPart("mainImages") MultipartFile[] mainImages,
                                             @RequestPart(value = "mainImages", required = false) MultipartFile[] exclusiveImages,
                                             @RequestPart(value = "recommendImages", required = false) MultipartFile[] recommendImages) throws IOException {

       return adminService.create(product, mainImages, exclusiveImages, recommendImages);
    }

    @PostMapping("/delete/{id}")
    @AdminOnly
    public ResponseEntity<Car> deleteProduct(@PathVariable Long id) {
        adminService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/options")
    @AdminOnly
    public ResponseEntity<Map<String, Object>> getListingOptions() {
        return adminService.getListingOptions();
    }

    @PostMapping
    @AdminOnly
    public ResponseEntity<?> createCar(
            @RequestPart("title") String title,
            @RequestPart("brand") String brand,
            @RequestPart("model") String model,
            @RequestPart("price") String price,
            @RequestPart("engineType") String engineType,
            @RequestPart("transmission") String transmission,
            @RequestPart("driveType") String driveType,
            @RequestPart("bodyType") String bodyType,
            @RequestPart("year") String year,
            @RequestPart("engineVolume") String engineVolume,
            @RequestPart("capacity") String capacity,
            @RequestPart("mileage") String mileage,
            @RequestPart(value = "exclusives", required = false) String exclusives,
            @RequestPart(value = "recommends", required = false) String recommends,
            @RequestPart("location") String location,
            @RequestPart("parameters") String parametersJson,
            @RequestPart(value = "main", required = false) MultipartFile[] mainFiles,
            @RequestPart(value = "exclusives", required = false) MultipartFile[] exclusiveFiles,
            @RequestPart(value = "recommends", required = false) MultipartFile[] recommendFiles
    ) {
        return adminService.tryCreatingCar(title, brand.toLowerCase(), model, price, engineType, transmission, driveType,
                bodyType, year, engineVolume, capacity, mileage, exclusives, recommends, location, parametersJson, mainFiles, exclusiveFiles, recommendFiles);
    }

    @PutMapping("/{id}")
    @AdminOnly
    public ResponseEntity<?> updateCar(
            @PathVariable Long id,
            @RequestPart("title") String title,
            @RequestPart("brand") String brand,
            @RequestPart("model") String model,
            @RequestPart("price") String price,
            @RequestPart("engineType") String engineType,
            @RequestPart("transmission") String transmission,
            @RequestPart("driveType") String driveType,
            @RequestPart("bodyType") String bodyType,
            @RequestPart("year") String year,
            @RequestPart("engineVolume") String engineVolume,
            @RequestPart("capacity") String capacity,
            @RequestPart("mileage") String mileage,
            @RequestPart(value = "exclusives", required = false) String exclusives,
            @RequestPart(value = "recommends", required = false) String recommends,
            @RequestPart("location") String location,
            @RequestPart("parameters") String parametersJson,
            @RequestPart(value = "oldMain", required = false) String oldMainJson,
            @RequestPart(value = "oldExclusives", required = false) String oldExclusivesJson,
            @RequestPart(value = "oldRecommends", required = false) String oldRecommendsJson,
            @RequestPart(value = "main", required = false) MultipartFile[] mainFiles,
            @RequestPart(value = "exclusives", required = false) MultipartFile[] exclusiveFiles,
            @RequestPart(value = "recommends", required = false) MultipartFile[] recommendFiles
    ) {
        return adminService.tryUpdateCar(id, title, brand.toLowerCase(), model, price, engineType, transmission, driveType, bodyType, year, engineVolume, capacity, mileage, exclusives, recommends, location, parametersJson, oldMainJson, oldExclusivesJson, oldRecommendsJson, mainFiles, exclusiveFiles, recommendFiles);
    }

    @GetMapping
    @AdminOnly
    public ResponseEntity<Page<CarDTO>> getAllCars(@RequestParam(required = false) Long search, Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllCars(search, pageable));
    }

    @GetMapping("/{id}")
    @AdminOnly
    public ResponseEntity<Map<String, Object>> getCarById(@PathVariable Long id) {
        CarDTO carDto = adminService.getCarById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("car", carDto);
        response.put("imageUrl", carDto.getImageUrl());

        return ResponseEntity.ok(response);
    }

}

