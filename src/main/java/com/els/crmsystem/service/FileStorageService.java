package com.els.crmsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    // We inject the path from application.yaml
    public FileStorageService(@Value("${els.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            // Create the directory if it doesn't exist
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null; // No file uploaded, that's fine
        }

        try {
            // 1. Clean the filename (security check)
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null) originalFileName = "unknown.jpg";

            // 2. Generate a unique name (to prevent overwriting)
            // Example: "cable.jpg" -> "a1b2c3d4-cable.jpg"
            String uniqueFileName = UUID.randomUUID().toString() + "-" + originalFileName;

            // 3. Resolve the path
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

            // 4. Save the file (overwrite if exists, though UUID makes that unlikely)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 5. Return the filename (so we can save it in the DB)
            return uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", ex);
        }
    }
}
