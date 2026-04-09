package com.edupulse.controller;

import com.edupulse.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
@RequestMapping("/principal")
public class PrincipalController {

    @Autowired 
    private FeedbackService feedbackService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "1") int semester, Model model) {
        
        Map<String, Object> data = feedbackService.getPrincipalDashboardData(semester);
        
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("semesters", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        model.addAttribute("totalFaculty", data.getOrDefault("totalFaculty", 0));
        model.addAttribute("totalResponses", data.getOrDefault("totalResponses", 0));
        model.addAttribute("institutionRating", data.getOrDefault("institutionRating", 0.0));
        model.addAttribute("activeStudents", data.getOrDefault("activeStudents", 0));
        model.addAttribute("vgPercentage", data.getOrDefault("vgPercentage", 0));
        model.addAttribute("gPercentage", data.getOrDefault("gPercentage", 0));
        model.addAttribute("avPercentage", data.getOrDefault("avPercentage", 0));
        model.addAttribute("baPercentage", data.getOrDefault("baPercentage", 0));
        model.addAttribute("facultyPerformance", data.getOrDefault("facultyPerformance", new ArrayList<>()));
        model.addAttribute("departmentPerformance", data.getOrDefault("departmentPerformance", new ArrayList<>()));
        
        return "principal/dashboard";
    }
}