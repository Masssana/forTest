package ru.college.carmarketplace.model.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CarFilter {

    private List<String> brands;
    private List<String> models;
    private Integer yearFrom = 0;
    private Integer yearTo;
    private Integer mileageFrom = 0;
    private Integer mileageTo = Integer.MAX_VALUE;
    private BigDecimal priceFrom = BigDecimal.valueOf(0);
    private BigDecimal priceTo = BigDecimal.valueOf(Integer.MAX_VALUE);
    private List<String> engineType;
    private List<String> bodyType;
    private List<String> driveType;
    private List<String> transmission;
    private String carType;
    private Short capacity;
    private Integer engineVolumeFrom = 0;
    private Integer engineVolumeTo = Integer.MAX_VALUE;
    private String sort;
    private String search;
    private String title;


    /**
     *             @RequestParam(required = false, defaultValue = "0") Integer page,
     *             @RequestParam(required = false, defaultValue = "20") Integer size,
     *             @RequestParam(required = false, defaultValue = "all") String condition,
     *             @RequestParam(required = false) String sortBy,
     *             @RequestParam(required = false, defaultValue = "desc") String sortDirection,
     *
     *
     */
}
