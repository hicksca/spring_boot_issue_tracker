package com.tracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tracked_issues")
public class TrackedIssue {
    @Id
    private Long id;
    private Long number;
    private String title;
    private String htmlUrl;
    private Instant createdAt;

    public TrackedIssue() {}

    public TrackedIssue(Long id, Long number, String title, String htmlUrl, Instant createdAt) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.htmlUrl = htmlUrl;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getNumber() { return number; }
    public String getTitle() { return title; }
    public String getHtmlUrl() { return htmlUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
