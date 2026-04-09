package com.edupulse.controller;

import com.edupulse.model.*;
import com.edupulse.service.FeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping("/start")
    public String startFeedback(@RequestParam int semester,
                                @RequestParam String branch,
                                @RequestParam String section,
                                HttpSession session, Model model) {

        String token = (String) session.getAttribute("feedbackToken");
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute("feedbackToken", token);
        }

        if (feedbackService.hasAlreadySubmitted(semester, branch, section, token)) {
            model.addAttribute("error", "You have already submitted feedback for this semester/branch/section.");
            model.addAttribute("semesters", FeedbackService.ALL_SEMESTERS);
            model.addAttribute("branches", FeedbackService.ALL_BRANCHES);
            model.addAttribute("sections", FeedbackService.ALL_SECTIONS);
            return "student/home";
        }

        List<SubjectAssignment> assignments = feedbackService.getAssignmentsForGroup(semester, branch, section);
        if (assignments.isEmpty()) {
            model.addAttribute("error", "No faculty assigned for this combination. Please contact the administrator.");
            model.addAttribute("semesters", FeedbackService.ALL_SEMESTERS);
            model.addAttribute("branches", FeedbackService.ALL_BRANCHES);
            model.addAttribute("sections", FeedbackService.ALL_SECTIONS);
            return "student/home";
        }

        session.setAttribute("semester", semester);
        session.setAttribute("branch", branch);
        session.setAttribute("section", section);

        model.addAttribute("assignments", assignments);
        model.addAttribute("criteria", FeedbackService.CRITERIA);
        model.addAttribute("semester", semester);
        model.addAttribute("branch", branch);
        model.addAttribute("section", section);
        model.addAttribute("ratingValues", FeedbackRating.Rating.values());

        return "student/feedback-form";
    }

    @PostMapping("/submit")
    public String submitFeedback(HttpSession session,
                                 @RequestParam Map<String, String> allParams,
                                 Model model) {

        Integer semester = (Integer) session.getAttribute("semester");
        String branch = (String) session.getAttribute("branch");
        String section = (String) session.getAttribute("section");
        String token = (String) session.getAttribute("feedbackToken");

        if (semester == null || branch == null || section == null) {
            return "redirect:/";
        }

        Map<String, Map<String, String>> ratingsMap = new LinkedHashMap<>();
        Map<String, String> comments = new HashMap<>();

        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("rating_")) {
                String[] parts = key.split("_", 3);
                if (parts.length == 3) {
                    String facultyId = parts[1];
                    String criterion = parts[2].replace("__", " & ").replace("_", " ");
                    ratingsMap.computeIfAbsent(facultyId, k -> new LinkedHashMap<>())
                              .put(criterion, entry.getValue());
                }
            } else if (key.startsWith("comment_")) {
                String facultyId = key.substring(8);
                comments.put(facultyId, entry.getValue());
            }
        }

        feedbackService.saveFeedback(semester, branch, section, token, ratingsMap, comments);
        session.removeAttribute("semester");
        session.removeAttribute("branch");
        session.removeAttribute("section");

        return "redirect:/feedback/success";
    }

    @GetMapping("/success")
    public String success() {
        return "student/success";
    }
}
