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

    public String saveFile(MultipartFile file) {
        return saveFileToSubfolder(file, "");
    }

    public String saveFileToSubfolder(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown_file");

        try {
            // Security Check
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
            }

            // 1. Create the specific subfolder path (e.g., uploads/users/1)
            Path targetDirectory = this.fileStorageLocation.resolve(subfolder).normalize();

            // 2. Automatically create the folder on the Windows/Linux hard drive if it doesn't exist
            Files.createDirectories(targetDirectory);

            // 3. Generate Unique Name
            String uniqueFileName = UUID.randomUUID().toString() + "-" + originalFileName;

            // 4. Resolve exact file path
            Path targetLocation = targetDirectory.resolve(uniqueFileName);

            // 5. Save the file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 6. Return the relative path so the DB saves "users/1/photo.jpg"
            if (subfolder.isEmpty()) {
                return uniqueFileName;
            }
            return subfolder + "/" + uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}