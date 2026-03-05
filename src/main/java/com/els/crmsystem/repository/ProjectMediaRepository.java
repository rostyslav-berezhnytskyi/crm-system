package com.els.crmsystem.repository;

import com.els.crmsystem.entity.ProjectMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMediaRepository extends JpaRepository<ProjectMedia, Long> {
    List<ProjectMedia> findByProjectId(Long projectId);
}
