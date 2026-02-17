package com.els.crmsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // Import Spring's utility
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${els.upload-dir}") String uploadDir) {
        // Normalizes path (e.g. changes 'src/../uploads' to just 'uploads')
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Clean the path (Sanitize filename)
        // This converts "..\..\virus.exe" to "virus.exe"
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown_file");

        try {
            // 2. Security Check: Block attempts to write outside the directory
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
            }

            // 3. Generate Unique Name
            // Preserves the extension (e.g., .pdf, .xlsx, .mp4)
            String uniqueFileName = UUID.randomUUID().toString() + "-" + originalFileName;

            // 4. Resolve Path
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            // 5. Save
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}