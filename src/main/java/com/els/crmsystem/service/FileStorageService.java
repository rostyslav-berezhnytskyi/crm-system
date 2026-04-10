package com.els.crmsystem.service;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
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

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(fileUrl).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Could not delete file: {}", fileUrl, e);
        }
    }
}