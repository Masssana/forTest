package ru.college.carmarketplace.service.filters;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.college.carmarketplace.model.dtos.CarFilter;
import ru.college.carmarketplace.service.CarSpecification;
import ru.college.carmarketplace.model.entities.Car;


@Service
public class DriveTypeCarFilter implements CarSpecification {
    @Override
    public Specification<Car> toSpecification(CarFilter filter) {
        if(filter.getDriveType() == null){
            return null;
        }
        return (root, cq, cb) -> root.get("driveType").in(filter.getDriveType());
    }
}
