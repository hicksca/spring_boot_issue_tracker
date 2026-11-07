package com.tracker.service;

import com.tracker.config.TrackerProperties;
import com.tracker.repository.TrackedIssueRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IssueTrackerSchedulerTest {

    private MockWebServer mockWebServer;

    @Autowired
    private TrackedIssueRepository repository;

    private TrackerProperties properties;
    private IssueTrackerScheduler scheduler;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new TrackerProperties();
        properties.setRepo("spring-projects/spring-boot");
        properties.setLabel("status: first-timers-only");
        properties.setPollIntervalMs(5000L);

        scheduler = new IssueTrackerScheduler(repository, properties);

        // Point the WebClient to the MockWebServer local port instead of api.github.com
        WebClient testWebClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();

        ReflectionTestUtils.setField(scheduler, "webClient", testWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
        repository.deleteAll();
    }

    @Test
    @DisplayName("Should detect, notify, and save new issue, but skip pull requests")
    void shouldProcessIssuesCorrectly() throws InterruptedException {
        // Mock GitHub response: 1 valid issue, 1 pull request
        String mockGithubResponse = """
            [
              {
                "id": 1001,
                "number": 1234,
                "title": "Fix doc typo for Beginners",
                "html_url": "https://github.com/spring-projects/spring-boot/issues/1234",
                "created_at": "2026-11-01T12:00:00Z"
              },
              {
                "id": 1002,
                "number": 1235,
                "title": "PR that has the label accidentally",
                "html_url": "https://github.com/spring-projects/spring-boot/pull/1235",
                "created_at": "2026-11-01T12:05:00Z",
                "pull_request": {
                  "url": "https://api.github.com/repos/spring-projects/spring-boot/pulls/1235"
                }
              }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockGithubResponse));

        scheduler.checkForIssues();

        // 1. Verify URL and query parameters sent by the client
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getPath()).contains("/repos/spring-projects/spring-boot/issues");
        assertThat(recordedRequest.getRequestUrl().queryParameter("labels")).isEqualTo("status: first-timers-only");

        // 2. Only the real issue should be saved, PR (id: 1002) is ignored
        assertThat(repository.existsById(1001L)).isTrue();
        assertThat(repository.existsById(1002L)).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not duplicate or re-save issues that are already seen")
    void shouldNotDuplicateSeenIssues() {
        // Enqueue identical response twice
        String mockGithubResponse = """
            [
              {
                "id": 2001,
                "number": 5555,
                "title": "First issue",
                "html_url": "https://github.com/spring-projects/spring-boot/issues/5555",
                "created_at": "2026-11-01T12:00:00Z"
              }
            ]
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockGithubResponse));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(mockGithubResponse));

        // First run saves the issue
        scheduler.checkForIssues();
        assertThat(repository.count()).isEqualTo(1);

        // Second run detects it already exists in the repository
        scheduler.checkForIssues();
        assertThat(repository.count()).isEqualTo(1);
    }
}