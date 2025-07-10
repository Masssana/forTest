package ru.college.carmarketplace.model.dtos;

import lombok.*;
import ru.college.carmarketplace.model.entities.CarImage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarImageDTO {
    private Long id;
    private String imageType; //
    private String imageUrl;

    public static CarImageDTO fromEntity(CarImage image) {
        return CarImageDTO.builder()
                .id(image.getId())
                .imageType(image.getImageType())
                .imageUrl(image.getImageUrl())
                .build();
    }
}