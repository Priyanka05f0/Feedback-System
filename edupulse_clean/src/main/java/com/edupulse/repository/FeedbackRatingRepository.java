package com.edupulse.repository;

import com.edupulse.model.FeedbackRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRatingRepository extends JpaRepository<FeedbackRating, Long> {
    
    List<FeedbackRating> findByFacultyId(Long facultyId);
    
    @Query("SELECT fr FROM FeedbackRating fr WHERE fr.faculty.id = :facultyId AND fr.submission.semester = :semester")
    List<FeedbackRating> findByFacultyIdAndSemester(@Param("facultyId") Long facultyId, @Param("semester") int semester);
    
    List<FeedbackRating> findBySubmissionId(Long submissionId);
}