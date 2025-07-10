package ru.college.carmarketplace.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.college.carmarketplace.service.MinioService;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final AmazonS3 s3;

    @Value("${minio.bucket}")
    private String bucket;

    public void uploadFile(MultipartFile file, String fileName){
        // Создаем метаданные с указанием размера файла
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // Загружаем файл напрямую из InputStream с метаданными
        try (InputStream inputStream = file.getInputStream()) {
            s3.putObject(
                    bucket,
                    fileName,
                    inputStream,
                    metadata
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFileUrl(String fileName) {
        return s3.getUrl(bucket, fileName).toString();
    }

}
