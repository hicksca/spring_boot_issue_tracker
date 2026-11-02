package com.tracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tracker")
public class TrackerProperties {
    private String repo;
    private String label;
    private long pollIntervalMs;
    private String githubToken;

    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }
}
