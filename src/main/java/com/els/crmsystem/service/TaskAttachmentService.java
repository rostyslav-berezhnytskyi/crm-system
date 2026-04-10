package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.TaskAttachmentOutputDto;
import com.els.crmsystem.entity.Task;
import com.els.crmsystem.entity.TaskAttachment;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.TaskAttachmentRepository;
import com.els.crmsystem.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final FileStorageService fileStorageService;
    private final EntityMapper mapper;

    @Transactional
    public void uploadAttachment(Long taskId, MultipartFile file) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // 1. Save physical file using your existing smart method
        String folderName = "tasks/" + taskId;
        String savedFilePath = fileStorageService.saveFileToSubfolder(file, folderName);

        // 2. Save database record
        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setFileUrl(savedFilePath);

        // Grab the original name (e.g. "invoice.pdf") so the user knows what they clicked
        String originalName = file.getOriginalFilename();
        attachment.setFileName(originalName != null ? originalName : "file");

        attachmentRepository.save(attachment);
    }

    // Don't forget to inject EntityMapper mapper; at the top!

    public List<TaskAttachmentOutputDto> getAttachmentsForTask(Long taskId) {
        return attachmentRepository.findByTaskId(taskId).stream()
                .map(mapper::toOutputDto)
                .toList();
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        // 1. Delete physical file from the Linux hard drive!
        fileStorageService.deleteFile(attachment.getFileUrl());

        // 2. Delete database record
        attachmentRepository.delete(attachment);
    }
}