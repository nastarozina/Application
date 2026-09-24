package ru.rozhi;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import ru.rozhi.configuration.StorageProperties;
import ru.rozhi.controller.dto.BannerRequest;
import ru.rozhi.controller.dto.BannerResponse;
import ru.rozhi.controller.dto.UploadUrlRequest;
import ru.rozhi.controller.dto.UploadUrlResponse;
import ru.rozhi.repository.BannerRepository;
import ru.rozhi.repository.model.Image;
import ru.rozhi.repository.model.ImageStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BannerRepository bannerRepository;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StorageProperties storageProperties;

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:8.3.11")
    );

    @Container
    static S3MockContainer s3Mock = new S3MockContainer("5.2.0")
            .withInitialBuckets("banner");

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.s3.endpoint", s3Mock::getHttpEndpoint);
    }

    @BeforeEach
    public void setUp() {
        bannerRepository.deleteAll();

        String bucket = storageProperties.getBucket();

        var response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .build()
        );

        if (response.contents().isEmpty()) {
            return;
        }

        var objects = response.contents().stream()
                .map(S3Object::key)
                .map(key -> ObjectIdentifier.builder()
                        .key(key)
                        .build())
                .toList();

        s3Client.deleteObjects(
                DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder()
                                .objects(objects)
                                .build())
                        .build()
        );
    }

    @Test
    @SneakyThrows
    public void checkImageUploadProcess() {
        BannerRequest bannerRequest = new BannerRequest("NAME1", "DESCRIPTION1");
        MvcResult createResult = mockMvc.perform(
                post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bannerRequest))
                )
                .andExpect(status().isCreated())
                .andReturn();

        String bannerId = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                        BannerResponse.class
                ).id();

        String contentType1 = "image/jpeg";
        UploadUrlRequest uploadUrlRequest1 = new UploadUrlRequest(contentType1);

        MvcResult uploadUrlResult1 = mockMvc.perform(
                        post("/{bannerId}/images/upload-url", bannerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(uploadUrlRequest1))
                )
                .andExpect(status().isOk())
                .andReturn();

        UploadUrlResponse uploadUrlResponse1 = objectMapper.readValue(
                uploadUrlResult1.getResponse().getContentAsString(),
                UploadUrlResponse.class
        );

        List<Image> images = bannerRepository.findById(bannerId).orElseThrow().getImages();
        assertThat(images).size().isEqualTo(1);
        assertThat(images.getFirst().getId()).isEqualTo(uploadUrlResponse1.imageId());
        assertThat(images.getFirst().getContentType()).isEqualTo(contentType1);
        assertThat(images.getFirst().getPosition()).isEqualTo(0);
        assertThat(images.getFirst().getSize()).isNull();
        assertThat(images.getFirst().getStatus()).isEqualTo(ImageStatus.PENDING);

        String contentType2 = "image/png";
        UploadUrlRequest uploadUrlRequest2 = new UploadUrlRequest(contentType2);

        MvcResult uploadUrlResult2 = mockMvc.perform(
                        post("/{bannerId}/images/upload-url", bannerId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(uploadUrlRequest2))
                )
                .andExpect(status().isOk())
                .andReturn();

        UploadUrlResponse uploadUrlResponse2 = objectMapper.readValue(
                uploadUrlResult2.getResponse().getContentAsString(),
                UploadUrlResponse.class
        );

        images = bannerRepository.findById(bannerId).orElseThrow().getImages();
        assertThat(images).size().isEqualTo(2);
        assertThat(images.getFirst().getId()).isEqualTo(uploadUrlResponse1.imageId());
        assertThat(images.getFirst().getContentType()).isEqualTo(contentType1);
        assertThat(images.getFirst().getPosition()).isEqualTo(0);
        assertThat(images.getFirst().getSize()).isNull();
        assertThat(images.getFirst().getStatus()).isEqualTo(ImageStatus.PENDING);
        assertThat(images.get(1).getId()).isEqualTo(uploadUrlResponse2.imageId());
        assertThat(images.get(1).getContentType()).isEqualTo(contentType2);
        assertThat(images.get(1).getPosition()).isEqualTo(1);
        assertThat(images.get(1).getSize()).isNull();
        assertThat(images.get(1).getStatus()).isEqualTo(ImageStatus.PENDING);

        uploadImage("__files/test-image.jpg", contentType1, uploadUrlResponse1.uploadUrl());

        mockMvc.perform(
                post("/{bannerId}/images/{imageId}/complete", bannerId, uploadUrlResponse1.imageId())
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk());

        images = bannerRepository.findById(bannerId).orElseThrow().getImages();
        assertThat(images).size().isEqualTo(2);
        assertThat(images.getFirst().getId()).isEqualTo(uploadUrlResponse1.imageId());
        assertThat(images.getFirst().getContentType()).isEqualTo(contentType1);
        assertThat(images.getFirst().getPosition()).isEqualTo(0);
        assertThat(images.getFirst().getSize()).isGreaterThan(0);
        assertThat(images.getFirst().getStatus()).isEqualTo(ImageStatus.UPLOADED);
        assertThat(images.get(1).getId()).isEqualTo(uploadUrlResponse2.imageId());
        assertThat(images.get(1).getContentType()).isEqualTo(contentType2);
        assertThat(images.get(1).getPosition()).isEqualTo(1);
        assertThat(images.get(1).getSize()).isNull();
        assertThat(images.get(1).getStatus()).isEqualTo(ImageStatus.PENDING);

        uploadImage("__files/test-image2.png", contentType2, uploadUrlResponse2.uploadUrl());

        mockMvc.perform(
                post("/{bannerId}/images/{imageId}/complete", bannerId, uploadUrlResponse2.imageId())
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk());

        images = bannerRepository.findById(bannerId).orElseThrow().getImages();
        assertThat(images).size().isEqualTo(2);
        assertThat(images.getFirst().getId()).isEqualTo(uploadUrlResponse1.imageId());
        assertThat(images.getFirst().getContentType()).isEqualTo(contentType1);
        assertThat(images.getFirst().getPosition()).isEqualTo(0);
        assertThat(images.getFirst().getSize()).isGreaterThan(0);
        assertThat(images.getFirst().getStatus()).isEqualTo(ImageStatus.UPLOADED);
        assertThat(images.get(1).getId()).isEqualTo(uploadUrlResponse2.imageId());
        assertThat(images.get(1).getContentType()).isEqualTo(contentType2);
        assertThat(images.get(1).getPosition()).isEqualTo(1);
        assertThat(images.getFirst().getSize()).isGreaterThan(0);
        assertThat(images.get(1).getStatus()).isEqualTo(ImageStatus.UPLOADED);
    }

    @SneakyThrows
    private void uploadImage(String fileName, String contentType, String presignedUrl) {
        byte[] imageBytes = new ClassPathResource(fileName)
                .getInputStream()
                .readAllBytes();

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest putRequest = HttpRequest.newBuilder()
                    .uri(URI.create(presignedUrl))
                    .header("Content-Type", contentType)
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(imageBytes))
                    .build();
            HttpResponse<String> putResponse = httpClient.send(
                    putRequest, HttpResponse.BodyHandlers.ofString()
            );

            assertThat(putResponse.statusCode())
                    .as("S3Mock должен принять загрузку по presigned URL")
                    .isIn(200, 201, 204);
        }
    }
}