package com.devops.inventory_service.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageService {

    @Value("${app.upload.dir:uploads}") // Lấy từ config, default là folder 'uploads'
    private String uploadDir;

    public String saveImage(MultipartFile file) throws IOException {
        // 1. Tạo folder nếu chưa có
        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        // 2. 🔥 HARDENING: Check Magic Number (Signature) 🔥
        // Đọc 4 byte đầu tiên của file để xem bản chất nó là gì
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);

            if (read < 4) throw new IOException("File corrupted or too short");

            // Check: Chỉ chấp nhận JPG hoặc PNG
            if (!isImageSignature(header)) {
                // Log cảnh báo ở đây sau này
                System.out.println("❌ [SECURITY ALERT] Phát hiện file giả mạo ảnh!");
                throw new IllegalArgumentException("Invalid file format! Only JPG/PNG allowed.");
            }
        }

        // 3. 🔥 HARDENING: Rename file (Chống ghi đè & Path Traversal) 🔥
        // Không bao giờ dùng tên gốc (file.getOriginalFilename)
        String extension = getExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + extension;

        // 4. Lưu file an toàn
        Path targetPath = root.resolve(newFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return newFileName; // Trả về tên mới để lưu xuống DB
    }

    // Helper: Check Hex Signature
    private boolean isImageSignature(byte[] header) {
        // JPG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) return true;
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) return true;

        return false;
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1) return ".jpg"; // Default
        return filename.substring(lastDot);
    }
}