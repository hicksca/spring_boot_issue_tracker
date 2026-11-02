package com.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueDto {
    private Long id;
    private Long number;
    private String title;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("pull_request")
    private Map<String, Object> pullRequest;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNumber() { return number; }
    public void setNumber(Long number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getPullRequest() { return pullRequest; }
    public void setPullRequest(Map<String, Object> pullRequest) { this.pullRequest = pullRequest; }

    public boolean isPullRequest() {
        return pullRequest != null;
    }
}
