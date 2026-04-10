package com.els.crmsystem.controller.web;

import com.els.crmsystem.service.CompanyService;
import com.els.crmsystem.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class WebPipelineController {

    private final CompanyService companyService;
    private final ContactService contactService;

    @GetMapping
    public String showPipelineDashboard(Model model) {
        model.addAttribute("pipelineCompanies", companyService.getPipelineCompanies());
        model.addAttribute("pipelineContacts", contactService.getPipelineContacts());
        return "pipeline/dashboard";
    }
}