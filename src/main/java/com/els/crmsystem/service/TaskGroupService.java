package com.els.crmsystem.service;

import com.els.crmsystem.entity.TaskGroup;
import com.els.crmsystem.repository.TaskGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskGroupService {

    private final TaskGroupRepository taskGroupRepository;

    public List<TaskGroup> getAllGroups() {
        return taskGroupRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional
    public TaskGroup createGroup(String name, String colorHex) {
        log.info("Creating new Task Group: {}", name);

        // Find out how many columns exist, so we can put this new one at the end
        long currentCount = taskGroupRepository.count();

        TaskGroup group = new TaskGroup();
        group.setName(name);
        group.setColorHex(colorHex != null ? colorHex : "#6c757d"); // Default to grey
        group.setDisplayOrder((int) currentCount + 1);

        return taskGroupRepository.save(group);
    }

    @Transactional
    public TaskGroup updateGroupName(Long id, String newName) {
        TaskGroup group = taskGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task Group not found"));

        group.setName(newName);
        return taskGroupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long id) {
        // DANGER: Because of CascadeType.ALL on the Entity,
        // deleting a group will automatically delete ALL tasks inside it!
        log.warn("Deleting Task Group ID: {}. All tasks inside it will be lost!", id);
        taskGroupRepository.deleteById(id);
    }

    @Transactional
    public void updateGroupPositions(List<Long> orderedGroupIds) {
        log.info("Reordering task groups: {}", orderedGroupIds);
        for (int i = 0; i < orderedGroupIds.size(); i++) {
            Long id = orderedGroupIds.get(i);

            // Standard fetch without lambda prevents the "effectively final" error
            TaskGroup group = taskGroupRepository.findById(id).orElse(null);
            if (group != null) {
                group.setDisplayOrder(i + 1); // 1, 2, 3...
                taskGroupRepository.save(group);
            }
        }
    }
}
