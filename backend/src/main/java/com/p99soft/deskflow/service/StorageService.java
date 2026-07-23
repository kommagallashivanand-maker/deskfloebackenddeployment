package com.p99soft.deskflow.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String uploadFile(MultipartFile file) throws IOException;
    String generatePresignedUrl(String fileUrl);
}
