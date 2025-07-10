package ru.college.carmarketplace.service.filters;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.college.carmarketplace.model.dtos.CarFilter;
import ru.college.carmarketplace.service.CarSpecification;
import ru.college.carmarketplace.model.entities.Car;

@Service
public class SortCarFilter implements CarSpecification {

    @Override
    public Specification<Car> toSpecification(CarFilter filter) {
        if(filter.getSort() != null){
            switch (filter.getSort()){
                case "cheaper":
                    return (root, query, cb) -> {
                        assert query != null;
                        query.orderBy(cb.asc(root.get("price")));
                        return null;
                    };
                case "expensive":
                    return (root, query, cb) -> {
                        assert query != null;
                        query.orderBy(cb.desc(root.get("price")));
                        return null;
                    };
                case "mileage":
                    return (root, query, cb) -> {
                        assert query != null;
                        query.orderBy(cb.asc(root.get("mileage")));
                        return null;
                    };
                case "createdAt":
                    return (root, query, cb) -> {
                        assert query != null;
                        query.orderBy(cb.desc(root.get("createAt")));
                        return null;
                    };

                case "newer":
                    return (root, query, cb) -> {
                        assert query != null;
                        query.orderBy(cb.desc(root.get("year")));
                        return null;
                    };
                case "older":
                    return (root, query, cb) ->{
                        assert query != null;
                        query.orderBy(cb.asc(root.get("year")));
                        return null;
                    };

            }
        }
        return null;
    }
}
