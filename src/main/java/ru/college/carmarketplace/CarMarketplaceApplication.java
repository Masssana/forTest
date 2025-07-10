package ru.college.carmarketplace;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.college.carmarketplace.model.entities.Car;
import ru.college.carmarketplace.repo.CarRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class CarMarketplaceApplication {

    public static void main(String[] args) {
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        SpringApplication.run(CarMarketplaceApplication.class, args);

    }


}
