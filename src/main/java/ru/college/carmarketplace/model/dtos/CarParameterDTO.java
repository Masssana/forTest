package ru.college.carmarketplace.model.dtos;

import lombok.*;
import ru.college.carmarketplace.model.entities.CarParameter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarParameterDTO {
    private Long id;
    private String image;
    private String title;
    private boolean available;

    public static CarParameterDTO fromEntity(CarParameter parameter) {
        return CarParameterDTO.builder()
                .image(parameter.getImage())
                .title(parameter.getTitle())
                .available(parameter.isAvailable())
                .build();
    }
}