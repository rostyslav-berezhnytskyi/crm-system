package com.els.crmsystem.service;

import com.els.crmsystem.dto.output.CompanyDocumentOutputDto;
import com.els.crmsystem.entity.Company;
import com.els.crmsystem.entity.CompanyDocument;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.CompanyDocumentRepository;
import com.els.crmsystem.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyDocumentService {

    private final CompanyDocumentRepository documentRepository;
    private final CompanyRepository companyRepository;
    private final FileStorageService fileStorageService;
    private final EntityMapper mapper;

    @Transactional
    public void uploadDocument(Long companyId, MultipartFile file, String description) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));

        // Use our smart file storage to automatically compress images and sort into folders
        String folderName = "companies/" + companyId;
        String savedFilePath = fileStorageService.saveFileToSubfolder(file, folderName);

        CompanyDocument document = new CompanyDocument();
        document.setCompany(company);
        document.setFileUrl(savedFilePath);
        document.setFileType(file.getContentType());
        document.setDescription(description != null ? description : "");

        documentRepository.save(document);
    }

    public List<CompanyDocumentOutputDto> getDocumentsForCompany(Long companyId) {
        return documentRepository.findByCompanyId(companyId).stream()
                .map(mapper::toOutputDto)
                .toList();
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        CompanyDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));

        fileStorageService.deleteFile(document.getFileUrl());
        documentRepository.delete(document);
    }
}
