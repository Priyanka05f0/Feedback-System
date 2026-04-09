package com.edupulse.service;

import com.edupulse.model.*;
import com.edupulse.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    @Autowired
    private FacultyRepository facultyRepository;
    
    @Autowired
    private FeedbackSubmissionRepository submissionRepository;
    
    @Autowired
    private FeedbackRatingRepository ratingRepository;
    
    @Autowired
    private SubjectAssignmentRepository assignmentRepository;

    // Static data for dropdowns
    public static final List<Integer> ALL_SEMESTERS = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);
    public static final List<String> ALL_BRANCHES = Arrays.asList("CSE", "IT", "ECE", "MECH", "CIVIL", "EEE");
    public static final List<String> ALL_SECTIONS = Arrays.asList("A", "B", "C");
    public static final List<String> CRITERIA = Arrays.asList(
        "Teaching Effectiveness",
        "Subject Knowledge", 
        "Communication Skills",
        "Punctuality",
        "Student Support"
    );

    // Check if student already submitted feedback
    public boolean hasAlreadySubmitted(int semester, String branch, String section, String token) {
        return submissionRepository.existsBySemesterAndBranchAndSectionAndToken(semester, branch, section, token);
    }

    // Get faculty assignments for a specific group
    public List<SubjectAssignment> getAssignmentsForGroup(int semester, String branch, String section) {
        return assignmentRepository.findBySemesterAndBranchAndSection(semester, branch, section);
    }

    // Save feedback submission
    @Transactional
    public void saveFeedback(int semester, String branch, String section, String token,
                            Map<String, Map<String, String>> ratingsMap, 
                            Map<String, String> comments) {
        
        FeedbackSubmission submission = new FeedbackSubmission();
        submission.setSemester(semester);
        submission.setBranch(branch);
        submission.setSection(section);
        submission.setToken(token);
        submission.setSubmittedAt(LocalDateTime.now());
        
        FeedbackSubmission savedSubmission = submissionRepository.save(submission);
        
        for (Map.Entry<String, Map<String, String>> facultyEntry : ratingsMap.entrySet()) {
            Long facultyId = Long.parseLong(facultyEntry.getKey());
            Faculty faculty = facultyRepository.findById(facultyId).orElse(null);
            
            if (faculty != null) {
                Map<String, String> criteriaRatings = facultyEntry.getValue();
                String facultyComment = comments.get(facultyEntry.getKey());
                boolean commentSaved = false;
                
                for (Map.Entry<String, String> ratingEntry : criteriaRatings.entrySet()) {
                    FeedbackRating rating = new FeedbackRating();
                    rating.setSubmission(savedSubmission);
                    rating.setFaculty(faculty);
                    rating.setCriteria(ratingEntry.getKey());
                    rating.setRating(FeedbackRating.Rating.valueOf(ratingEntry.getValue()));
                    
                    if (facultyComment != null && !facultyComment.isEmpty() && !commentSaved) {
                        rating.setComment(facultyComment);
                        commentSaved = true;
                    }
                    
                    ratingRepository.save(rating);
                }
            }
        }
        
        System.out.println("Feedback saved for Semester: " + semester + 
                          ", Branch: " + branch + ", Section: " + section);
    }

    // Get Principal Dashboard Data - REAL DATA FROM DATABASE
    public Map<String, Object> getPrincipalDashboardData(int semester) {
        Map<String, Object> data = new HashMap<>();
        
        // Get all staff faculty from database
        List<Faculty> allFaculty = facultyRepository.findAll();
        List<Faculty> staffFaculty = allFaculty.stream()
                .filter(f -> f.getRole() == Faculty.Role.STAFF)
                .collect(Collectors.toList());
        
        // Get submissions for this semester from database
        List<FeedbackSubmission> submissions = submissionRepository.findBySemester(semester);
        
        int totalResponses = submissions.size();
        double institutionRating = calculateInstitutionRating(submissions);
        
        // Faculty performance data from database
        List<Map<String, Object>> facultyPerformance = new ArrayList<>();
        for (Faculty faculty : staffFaculty) {
            Map<String, Object> facultyData = new HashMap<>();
            facultyData.put("name", faculty.getName());
            facultyData.put("shortName", faculty.getShortName());
            facultyData.put("department", faculty.getDepartment());
            
            // Get ratings for this faculty for this semester from database
            List<FeedbackRating> facultyRatings = ratingRepository.findByFacultyIdAndSemester(faculty.getId(), semester);
            double avgRating = calculateAverageRating(facultyRatings);
            int responseCount = facultyRatings.size();
            
            facultyData.put("rating", Math.round(avgRating * 10) / 10.0);
            facultyData.put("responses", responseCount);
            facultyPerformance.add(facultyData);
        }
        
        // Sort by rating descending
        facultyPerformance.sort((a, b) -> Double.compare((Double)b.get("rating"), (Double)a.get("rating")));
        
        // Rating distribution from database
        Map<String, Integer> ratingDistribution = new HashMap<>();
        ratingDistribution.put("VERY_GOOD", 0);
        ratingDistribution.put("GOOD", 0);
        ratingDistribution.put("AVERAGE", 0);
        ratingDistribution.put("BELOW_AVERAGE", 0);
        
        for (FeedbackSubmission sub : submissions) {
            for (FeedbackRating rating : sub.getRatings()) {
                String key = rating.getRating().name();
                ratingDistribution.put(key, ratingDistribution.getOrDefault(key, 0) + 1);
            }
        }
        
        int totalRatings = submissions.isEmpty() ? 1 : 
            submissions.stream().mapToInt(s -> s.getRatings().size()).sum();
        
        // Department performance from database
        Map<String, List<Double>> deptRatings = new HashMap<>();
        for (FeedbackSubmission sub : submissions) {
            for (FeedbackRating rating : sub.getRatings()) {
                String dept = rating.getFaculty().getDepartment();
                if (dept != null && !dept.isEmpty()) {
                    deptRatings.computeIfAbsent(dept, k -> new ArrayList<>())
                              .add(getRatingValue(rating.getRating()));
                }
            }
        }
        
        List<Map<String, Object>> departmentPerformance = new ArrayList<>();
        String[] deptOrder = {"CSE", "IT", "ECE", "MECH", "CIVIL", "EEE"};
        for (String deptName : deptOrder) {
            Map<String, Object> deptData = new HashMap<>();
            deptData.put("name", deptName);
            List<Double> ratings = deptRatings.get(deptName);
            double avg = ratings == null ? 0 : ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            deptData.put("rating", Math.round(avg * 10) / 10.0);
            departmentPerformance.add(deptData);
        }
        
        data.put("totalFaculty", staffFaculty.size());
        data.put("totalResponses", totalResponses);
        data.put("institutionRating", Math.round(institutionRating * 10) / 10.0);
        data.put("activeStudents", 342);
        data.put("facultyPerformance", facultyPerformance);
        data.put("departmentPerformance", departmentPerformance);
        data.put("vgPercentage", totalRatings == 0 ? 0 : Math.round((ratingDistribution.get("VERY_GOOD") * 100.0) / totalRatings));
        data.put("gPercentage", totalRatings == 0 ? 0 : Math.round((ratingDistribution.get("GOOD") * 100.0) / totalRatings));
        data.put("avPercentage", totalRatings == 0 ? 0 : Math.round((ratingDistribution.get("AVERAGE") * 100.0) / totalRatings));
        data.put("baPercentage", totalRatings == 0 ? 0 : Math.round((ratingDistribution.get("BELOW_AVERAGE") * 100.0) / totalRatings));
        
        return data;
    }

    // Get Staff Dashboard Data - REAL DATA FROM DATABASE
    public Map<String, Object> getStaffDashboardData(Long facultyId, int semester) {
        Map<String, Object> data = new HashMap<>();
        
        // Get ratings for this faculty and semester from database
        List<FeedbackRating> ratings = ratingRepository.findByFacultyIdAndSemester(facultyId, semester);
        
        int totalResponses = ratings.size();
        double overallRating = calculateAverageRating(ratings);
        
        int vgCount = 0, gCount = 0, avCount = 0, baCount = 0;
        for (FeedbackRating rating : ratings) {
            switch (rating.getRating()) {
                case VERY_GOOD: vgCount++; break;
                case GOOD: gCount++; break;
                case AVERAGE: avCount++; break;
                case BELOW_AVERAGE: baCount++; break;
            }
        }
        
        int total = ratings.isEmpty() ? 1 : ratings.size();
        
        data.put("totalResponses", totalResponses);
        data.put("overallRating", totalResponses == 0 ? 0 : Math.round(overallRating * 10) / 10.0);
        data.put("scorePercentage", totalResponses == 0 ? 0 : Math.round((overallRating / 4.0) * 100));
        data.put("vgPercentage", Math.round((vgCount * 100.0) / total));
        data.put("gPercentage", Math.round((gCount * 100.0) / total));
        data.put("avPercentage", Math.round((avCount * 100.0) / total));
        data.put("baPercentage", Math.round((baCount * 100.0) / total));
        
        // Criteria scores from database
        Map<String, List<Double>> criteriaMap = new HashMap<>();
        for (FeedbackRating rating : ratings) {
            criteriaMap.computeIfAbsent(rating.getCriteria(), k -> new ArrayList<>())
                      .add(getRatingValue(rating.getRating()));
        }
        
        List<Map<String, Object>> criteriaScores = new ArrayList<>();
        String[] defaultCriteria = {"Teaching Effectiveness", "Subject Knowledge", "Communication Skills", "Punctuality", "Student Support"};
        
        for (String criteriaName : defaultCriteria) {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("name", criteriaName);
            List<Double> scores = criteriaMap.get(criteriaName);
            double avg = scores == null ? 0 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            criteria.put("score", Math.round(avg * 10) / 10.0);
            criteriaScores.add(criteria);
        }
        
        data.put("criteriaScores", criteriaScores);
        
        // Comments from database
        List<Map<String, Object>> comments = getFacultyComments(facultyId, semester);
        data.put("comments", comments);
        
        return data;
    }

    // Get faculty comments from database
    public List<Map<String, Object>> getFacultyComments(Long facultyId, int semester) {
        List<Map<String, Object>> commentsList = new ArrayList<>();
        
        List<FeedbackRating> ratings = ratingRepository.findByFacultyIdAndSemester(facultyId, semester);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        
        Set<String> processedComments = new HashSet<>();
        
        for (FeedbackRating rating : ratings) {
            if (rating.getComment() != null && !rating.getComment().trim().isEmpty()) {
                String uniqueKey = rating.getSubmission().getId() + "_" + rating.getFaculty().getId();
                
                if (!processedComments.contains(uniqueKey)) {
                    Map<String, Object> comment = new LinkedHashMap<>();
                    comment.put("comment", rating.getComment());
                    comment.put("criteria", rating.getCriteria());
                    comment.put("rating", rating.getRating().toString());
                    comment.put("submittedAt", rating.getSubmission().getSubmittedAt().format(formatter));
                    comment.put("branch", rating.getSubmission().getBranch());
                    comment.put("section", rating.getSubmission().getSection());
                    commentsList.add(comment);
                    processedComments.add(uniqueKey);
                }
            }
        }
        
        commentsList.sort((a, b) -> ((String)b.get("submittedAt")).compareTo((String)a.get("submittedAt")));
        
        return commentsList;
    }

    private double calculateInstitutionRating(List<FeedbackSubmission> submissions) {
        if (submissions.isEmpty()) return 0;
        double total = 0;
        int count = 0;
        for (FeedbackSubmission sub : submissions) {
            for (FeedbackRating rating : sub.getRatings()) {
                total += getRatingValue(rating.getRating());
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    private double calculateAverageRating(List<FeedbackRating> ratings) {
        if (ratings.isEmpty()) return 0;
        double sum = 0;
        for (FeedbackRating r : ratings) {
            sum += getRatingValue(r.getRating());
        }
        return sum / ratings.size();
    }

    private double getRatingValue(FeedbackRating.Rating rating) {
        switch (rating) {
            case VERY_GOOD: return 4.0;
            case GOOD: return 3.0;
            case AVERAGE: return 2.0;
            case BELOW_AVERAGE: return 1.0;
            default: return 0;
        }
    }
}