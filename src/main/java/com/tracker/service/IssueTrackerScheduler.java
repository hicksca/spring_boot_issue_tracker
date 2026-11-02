package com.tracker.service;

import com.tracker.config.TrackerProperties;
import com.tracker.model.GitHubIssueDto;
import com.tracker.model.TrackedIssue;
import com.tracker.repository.TrackedIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class IssueTrackerScheduler {
    private static final Logger log = LoggerFactory.getLogger(IssueTrackerScheduler.class);

    private final TrackedIssueRepository repository;
    private final TrackerProperties properties;
    private final WebClient webClient;

    public IssueTrackerScheduler(TrackedIssueRepository repository, TrackerProperties properties) {
        this.repository = repository;
        this.properties = properties;

        WebClient.Builder builder = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .defaultHeader("User-Agent", "spring-boot-issue-tracker");

        if (properties.getGithubToken() != null && !properties.getGithubToken().isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + properties.getGithubToken());
        }

        this.webClient = builder.build();
    }

    @Scheduled(fixedDelayString = "${tracker.poll-interval-ms:300000}")
    public void checkForIssues() {
        log.info("Checking repository '{}' for issues with label '{}'...",
                properties.getRepo(), properties.getLabel());

        try {
            List<GitHubIssueDto> issues = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/" + properties.getRepo() + "/issues")
                            .queryParam("labels", properties.getLabel())
                            .queryParam("state", "open")
                            .queryParam("sort", "created")
                            .queryParam("direction", "desc")
                            .build())
                    .retrieve()
                    .bodyToFlux(GitHubIssueDto.class)
                    .collectList()
                    .block();

            if (issues == null || issues.isEmpty()) {
                return;
            }

            for (GitHubIssueDto issue : issues) {
                if (issue.isPullRequest()) {
                    continue;
                }

                if (!repository.existsById(issue.getId())) {
                    notify(issue);
                    repository.save(new TrackedIssue(
                            issue.getId(),
                            issue.getNumber(),
                            issue.getTitle(),
                            issue.getHtmlUrl(),
                            issue.getCreatedAt()
                    ));
                }
            }
        } catch (Exception ex) {
            log.error("Failed to fetch or process GitHub issues: {}", ex.getMessage());
        }
    }

    private void notify(GitHubIssueDto issue) {
        log.info("\n========================================\n" +
                 "[NEW FIRST-TIMERS ISSUE DETECTED]\n" +
                 "Issue: #{} - {}\n" +
                 "URL: {}\n" +
                 "Created: {}\n" +
                 "========================================",
                issue.getNumber(), issue.getTitle(), issue.getHtmlUrl(), issue.getCreatedAt());
    }
}
