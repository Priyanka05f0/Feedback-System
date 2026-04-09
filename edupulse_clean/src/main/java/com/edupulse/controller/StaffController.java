package com.edupulse.controller;

import com.edupulse.model.Faculty;
import com.edupulse.repository.FacultyRepository;
import com.edupulse.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired private FeedbackService feedbackService;
    @Autowired private FacultyRepository facultyRepo;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "1") int semester,
                             Model model) {
        
        if (userDetails == null || userDetails.getUsername() == null) {
            return "redirect:/login";
        }
        
        Optional<Faculty> facultyOpt = facultyRepo.findByUsername(userDetails.getUsername());
        if (facultyOpt.isEmpty()) {
            return "redirect:/login?error=true";
        }
        
        Faculty faculty = facultyOpt.get();
        
        Map<String, Object> dashData = feedbackService.getStaffDashboardData(faculty.getId(), semester);
        
        model.addAttribute("faculty", faculty);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("semesters", Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8));
        model.addAttribute("totalResponses", dashData.getOrDefault("totalResponses", 0));
        model.addAttribute("overallRating", dashData.getOrDefault("overallRating", 0.0));
        model.addAttribute("scorePercentage", dashData.getOrDefault("scorePercentage", 0));
        model.addAttribute("vgPercentage", dashData.getOrDefault("vgPercentage", 0));
        model.addAttribute("gPercentage", dashData.getOrDefault("gPercentage", 0));
        model.addAttribute("avPercentage", dashData.getOrDefault("avPercentage", 0));
        model.addAttribute("baPercentage", dashData.getOrDefault("baPercentage", 0));
        model.addAttribute("criteriaScores", dashData.getOrDefault("criteriaScores", new ArrayList<>()));
        model.addAttribute("comments", dashData.getOrDefault("comments", new ArrayList<>()));
        
        return "staff/dashboard";
    }
}