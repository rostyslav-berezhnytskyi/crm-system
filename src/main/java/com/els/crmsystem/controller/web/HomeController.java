package com.els.crmsystem.controller.web;

import com.els.crmsystem.dto.output.ProjectOutputDto;
import com.els.crmsystem.dto.output.TransactionOutputDto;
import com.els.crmsystem.service.ExchangeRateService;
import com.els.crmsystem.service.FinanceService;
import com.els.crmsystem.service.ProjectService;
import com.els.crmsystem.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProjectService projectService;
    private final TransactionService transactionService;
    private final FinanceService financeService;
    private final ExchangeRateService exchangeRateService;

    @GetMapping("/")
    public String showDashboard(Model model) {
        // 1. Fetch all transactions (using your existing filter method with all nulls)
        List<TransactionOutputDto> allTransactions = transactionService.getAllFilteredTransactions(
                null, null, null, null, null, null, null, null, null);

        // 2. Calculate Global Finances
        model.addAttribute("totalBalance", financeService.calculateTotalBalance(allTransactions));
        model.addAttribute("totalIncome", financeService.calculateTotalIncome(allTransactions));
        model.addAttribute("totalExpense", financeService.calculateTotalExpense(allTransactions));
        model.addAttribute("exchangeRates", exchangeRateService.getCurrentRates());

        // 3. Get Top 5 Most Recent Transactions
        List<TransactionOutputDto> recentTransactions = allTransactions.stream()
                .sorted(Comparator.comparing(TransactionOutputDto::date).reversed())
                .limit(5)
                .toList();
        model.addAttribute("recentTransactions", recentTransactions);

        // 4. Get All Active Projects
        List<ProjectOutputDto> allActiveProjects = projectService.getAllActiveProjects();

        // 4a. Top 5 for the sidebar list
        List<ProjectOutputDto> recentActiveProjects = allActiveProjects.stream()
                .sorted(Comparator.comparing(ProjectOutputDto::createdDate).reversed())
                .limit(5)
                .toList();
        model.addAttribute("activeProjects", recentActiveProjects);

        // 4b. ALL projects with GPS coordinates for the Map
        List<ProjectOutputDto> mapProjects = allActiveProjects.stream()
                .filter(p -> p.latitude() != null && p.longitude() != null)
                .toList();
        model.addAttribute("mapProjects", mapProjects);

        return "index";
    }
}
