package com.els.crmsystem.service;

import com.els.crmsystem.dto.TransactionDto;
import com.els.crmsystem.entity.Project;
import com.els.crmsystem.entity.Transaction;
import com.els.crmsystem.entity.User;
import com.els.crmsystem.repository.ProjectRepository;
import com.els.crmsystem.repository.TransactionRepository;
import com.els.crmsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createTransaction(TransactionDto dto, String username) {

        // 1. Find the Project (Throw error if not found)
        Project project = projectRepository.findById(dto.projectId())
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.projectId()));

        // 2. Find the User (We use the username from the logged-in session)
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));

        // 3. Create the Transaction Entity
        Transaction transaction = new Transaction();
        transaction.setType(dto.type());
        transaction.setAmount(dto.amount());
        transaction.setCategory(dto.category());
        transaction.setPaymentMethod(dto.paymentMethod());
        transaction.setDescription(dto.description());

        // 4. Set Relationships
        transaction.setProject(project);
        transaction.setUser(user);

        // 5. Save to Database
        transactionRepository.save(transaction);
    }
}
