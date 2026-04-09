package com.edupulse.controller;

import com.edupulse.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("semesters", FeedbackService.ALL_SEMESTERS);
        model.addAttribute("branches", FeedbackService.ALL_BRANCHES);
        model.addAttribute("sections", FeedbackService.ALL_SECTIONS);
        return "student/home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}