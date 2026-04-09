package com.edupulse.model;

import jakarta.persistence.*;

@Entity
@Table(name = "feedback_rating")
public class FeedbackRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "submission_id")
    private FeedbackSubmission submission;

    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    private String criteria;

    @Enumerated(EnumType.STRING)
    private Rating rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    public enum Rating {
        VERY_GOOD, GOOD, AVERAGE, BELOW_AVERAGE
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FeedbackSubmission getSubmission() { return submission; }
    public void setSubmission(FeedbackSubmission submission) { this.submission = submission; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }
    public Rating getRating() { return rating; }
    public void setRating(Rating rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
