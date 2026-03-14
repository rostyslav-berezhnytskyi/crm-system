package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.ProjectMediaOutputDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.entity.ProjectMedia;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.ProjectMediaRepository;
import com.els.crmsystem.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectMediaService {

    private final ProjectMediaRepository mediaRepository;
    private final ProjectRepository projectRepository;
    private final FileStorageService fileStorageService;
    private final EntityMapper mapper;

    @Transactional
    public void uploadMedia(Long projectId, MultipartFile file, String description) {
        // 1. Find the project
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));

        // 2. Save the physical file using your smart subfolder trick
        String folderName = "projects/" + projectId;
        String savedFilePath = fileStorageService.saveFileToSubfolder(file, folderName);

        // 3. Create the database record
        ProjectMedia media = new ProjectMedia();
        media.setProject(project);
        media.setFileUrl(savedFilePath);
        media.setFileType(file.getContentType()); // Grabs "image/jpeg" or "video/mp4" automatically
        media.setDescription(description != null ? description : "");

        mediaRepository.save(media);
    }

    public List<ProjectMediaOutputDto> getGalleryForProject(Long projectId) {
        return mediaRepository.findByProjectId(projectId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMedia(Long mediaId) {
        ProjectMedia media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found"));

        // Note: This deletes the database record.
        // We can add a method to FileStorageService later to delete the actual file from the Linux drive if we want!
        mediaRepository.delete(media);
    }
}
