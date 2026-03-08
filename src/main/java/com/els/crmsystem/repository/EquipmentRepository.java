package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    List<Equipment> findByProjectId(Long projectId);
}