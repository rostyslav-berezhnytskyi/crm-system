package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.EquipmentInputDto;
import com.els.crmsystem.dto.output.EquipmentOutputDto;
import com.els.crmsystem.entity.Equipment;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.EquipmentRepository;
import com.els.crmsystem.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ProjectRepository projectRepository;
    private final EntityMapper mapper;

    @Transactional
    public void addEquipmentToProject(Long projectId, EquipmentInputDto dto) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Equipment equipment = new Equipment();
        equipment.setProject(project);
        equipment.setName(dto.name());
        equipment.setSerialNumber(dto.serialNumber());
        equipment.setWarrantyMonths(dto.warrantyMonths());
        equipment.setNotes(dto.notes());

        equipmentRepository.save(equipment);
    }

    public List<EquipmentOutputDto> getEquipmentForProject(Long projectId) {
        return equipmentRepository.findByProjectId(projectId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEquipment(Long equipmentId) {
        equipmentRepository.deleteById(equipmentId);
    }
}
