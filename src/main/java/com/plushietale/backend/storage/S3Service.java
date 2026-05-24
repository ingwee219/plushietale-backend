package com.plushietale.backend.storage;

import com.plushietale.backend.global.exception.CustomException;
import com.plushietale.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * S3에 파일 업로드 후 URL 반환
     * @param file 업로드할 파일
     * @param folder S3 내 저장 폴더 (예: "toys")
     * @return S3 public URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage());
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    /**
     * S3에서 파일 삭제
     * @param imageUrl S3 URL (uploadFile이 반환한 URL)
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;

        // URL에서 key 추출: "https://bucket.s3.region.amazonaws.com/key" → "key"
        String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * S3에서 파일 bytes 다운로드 (Gemini API에 이미지 전달용)
     * @param imageUrl S3 URL
     * @return 이미지 바이트 배열
     */
    public byte[] downloadFile(String imageUrl) {
        String key = imageUrl.substring(imageUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());

        return s3Client.getObjectAsBytes(req -> req.bucket(bucket).key(key)).asByteArray();
    }
}
