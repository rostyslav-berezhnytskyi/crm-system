package com.els.crmsystem.service;

import net.coobird.thumbnailator.Thumbnails;
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
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + originalFileName);
            }

            // 1. Create the specific subfolder path
            Path targetDirectory = this.fileStorageLocation.resolve(subfolder).normalize();
            Files.createDirectories(targetDirectory);

            // 2. Generate Unique Name (Ensuring we keep the extension)
            String extension = originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : ".jpg";
            String uniqueFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = targetDirectory.resolve(uniqueFileName);

            // 3. SMART COMPRESSION LOGIC
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                // If it's a photo, compress it!
                Thumbnails.of(file.getInputStream())
                        .size(1920, 1080)
                        .outputQuality(0.7)
                        .toFile(targetLocation.toFile());
            } else {
                // If it's a PDF or Excel file, save it normally
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Return the relative path
            if (subfolder.isEmpty()) {
                return uniqueFileName;
            }
            return subfolder + "/" + uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Saves a file to /uploads/projects/{projectId}/transactions/
     * Automatically compresses images to save disk space.
     */
    public String saveTransactionFile(MultipartFile file, Long projectId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            // 1. Create the smart folder path relative to your main upload dir
            String subfolder = "projects/" + projectId + "/transactions";
            Path uploadPath = this.fileStorageLocation.resolve(subfolder).normalize();

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath); // Creates missing folders automatically
            }

            // 2. Generate a safe, unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg"; // fallback extension

            String uniqueFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(uniqueFilename);

            // 3. Compress if it's an image, otherwise save normally
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                // Compress images to 1080p max and 70% JPEG quality
                Thumbnails.of(file.getInputStream())
                        .size(1920, 1080)
                        .outputQuality(0.7)
                        .toFile(filePath.toFile());
            } else {
                // It's a PDF or something else, save it exactly as is
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Return the relative path to save in the Database
            return subfolder + "/" + uniqueFilename;

        } catch (IOException e) {
            throw new RuntimeException("Не вдалося зберегти файл транзакції", e);
        }
    }
}