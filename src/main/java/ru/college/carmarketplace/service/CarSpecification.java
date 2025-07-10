package ru.college.carmarketplace.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.college.carmarketplace.model.dtos.CarFilter;
import ru.college.carmarketplace.model.entities.Car;

@Service
public interface CarSpecification {
    Specification<Car> toSpecification(CarFilter filter);
}
