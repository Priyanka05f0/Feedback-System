package com.edupulse.repository;

import com.edupulse.model.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, Long> {
    
    List<FeedbackSubmission> findBySemester(int semester);
    
    boolean existsBySemesterAndBranchAndSectionAndToken(int semester, String branch, String section, String token);
}