package ru.college.carmarketplace.repo;

import org.springframework.data.repository.CrudRepository;
import ru.college.carmarketplace.model.entities.SvgImages;

import java.util.List;

public interface SvgImagesRepository extends CrudRepository<SvgImages, Long> {
    SvgImages findDataByName(String name);
}
