package ru.college.carmarketplace.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import ru.college.carmarketplace.model.dtos.CarDTO;
import ru.college.carmarketplace.model.dtos.CarParameterDTO;
import ru.college.carmarketplace.model.dtos.OrderDTO;
import ru.college.carmarketplace.model.entities.Car;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AdminService {

    void deleteProduct(Long id);

    void updateCar(
            Long id,
            CarDTO carDTO,
            MultipartFile[] mainFiles,
            MultipartFile[] exclusiveFiles,
            MultipartFile[] recommendFiles,
            List<String> oldMain,
            List<String> oldExclusives,
            List<String> oldRecommends
    ) throws IOException;

    List<OrderDTO> getReadyOrders(String search);

    List<OrderDTO> getNewOrders(String search);

    List<OrderDTO> getNotReadyOrders(String search);

    String uploadImage(MultipartFile file);

    ResponseEntity<Car> create( CarDTO product, MultipartFile[] mainImages,
                                       MultipartFile[] exclusiveImages,
                                       MultipartFile[] recommendImages) throws IOException;

    Page<CarDTO> getAllCars(Long search, Pageable pageable);

    CarDTO getCarById(Long id);


    ResponseEntity<Map<String, Object>> getListingOptions();

    ResponseEntity<?> tryCreatingCar(String title,
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
                                            MultipartFile[] recommendFiles);

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
                                          MultipartFile[] recommendFiles);

}
