package com.els.crmsystem.repository;

import com.els.crmsystem.entity.TaskGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {

    // Automatically fetches columns in the correct left-to-right order
    List<TaskGroup> findAllByOrderByDisplayOrderAsc();
}
