package com.tu.backend.storage;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.storage.dto.FileUploadResponseDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
@ConditionalOnProperty(prefix = "storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponseDto> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(fileStorageService.upload(file));
    }

    @GetMapping({"/{id}", "/{id}/"})
    public ResponseEntity<StreamingResponseBody> download(@PathVariable("id") String id) {
        FileStorageService.StoredFile stored = fileStorageService.open(id);
        StreamingResponseBody body = outputStream -> {
            try (InputStream input = stored.stream()) {
                input.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .contentType(MediaType.parseMediaType(stored.contentType()))
            .contentLength(stored.sizeBytes())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + stored.filename() + "\"")
            .body(body);
    }
}
