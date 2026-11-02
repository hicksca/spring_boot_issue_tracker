# Spring Boot Issue Tracker (MVP)

A containerized Spring Boot service that monitors GitHub repositories for first-timer issues (`status: first-timers-only`) and logs/alerts when a match is found. Seen issues are persisted to an embedded file-backed H2 database to prevent duplicate alerts across restarts.

---

## Features

- **GitHub REST API Polling**: Periodically queries GitHub for open issues with target labels (ignoring pull requests).
- **Persistent Storage**: Uses Spring Data JPA and an embedded H2 database saved to disk/volumes to track seen issues.
- **Dockerized**: Multi-stage Docker build targeting lightweight Alpine images on OpenJDK / Java 21 (`aarch64` & `x86_64`).
- **Slack Alert Ready**: Formatted notification pipeline ready to integrate with incoming Slack webhooks.

---

## Project Structure

```text
spring_boot_issue_tracker/
├── Dockerfile
├── docker-compose.yml
├── build.gradle
├── settings.gradle
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── tracker/
│       │           ├── TrackerApplication.java
│       │           ├── config/
│       │           │   └── TrackerProperties.java
│       │           ├── model/
│       │           │   ├── GitHubIssueDto.java
│       │           │   └── TrackedIssue.java
│       │           ├── repository/
│       │           │   └── TrackedIssueRepository.java
│       │           └── service/
│       │               └── IssueTrackerScheduler.java
│       └── resources/
│           └── application.properties
└── README.md
```

---

## Prerequisites

- **Java**: OpenJDK 21 (Amazon Corretto, Eclipse Temurin, or similar)
- **Gradle**: 8.6+ (or using `./gradlew`)
- **Docker & Docker Compose**: Modern Docker Desktop or CLI engine

---

## Configuration

Configuration values are mapped to `application.properties` and can be overridden via environment variables:

| Variable | Property Key | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `DB_PATH` | `spring.datasource.url` | `./data/issues_db` | Path where H2 database writes persistent files (`/data/issues_db` inside Docker). |
| `TRACKER_REPO` | `tracker.repo` | `spring-projects/spring-boot` | Target repository in `owner/repo` format. |
| `TRACKER_LABEL` | `tracker.label` | `status: first-timers-only` | Target issue label to monitor. |
| `TRACKER_POLL_INTERVAL_MS` | `tracker.poll-interval-ms` | `300000` (5 min) | Polling interval in milliseconds. |
| `TRACKER_GITHUB_TOKEN` | `tracker.github-token` | *(empty)* | Optional Personal Access Token to raise rate limits (60/hr to 5,000/hr). |

---

## Running Locally (IntelliJ IDEA or CLI)

1. **Build and Run via Gradle:**
   ```bash
   ./gradlew bootRun
   ```

2. **Run in IntelliJ IDEA:**
   - Open the project directory in IntelliJ.
   - Set the Project SDK to Java 21 (**File** > **Project Structure** > **Project**).
   - Locate `com.tracker.TrackerApplication` and click the green **Run (▶)** button.

Database files will automatically be created in `./data/issues_db.mv.db` in your local project directory.

---

## Running with Docker Compose

1. **Build and launch container in the background:**
   ```bash
   docker compose up -d --build
   ```

2. **Inspect application logs:**
   ```bash
   docker compose logs -f
   ```

3. **Stop the tracker:**
   ```bash
   docker compose down
   ```

Data persists between container rebuilds via the named volume `tracker-data` mounted at `/data`.

---

## How to Test Issue Detection

Because `spring-projects/spring-boot` rarely has open `status: first-timers-only` issues, you can verify alert triggering and database deduplication by temporarily modifying the target label:

1. Update `TRACKER_LABEL` in `docker-compose.yml` (or `tracker.label` in `application.properties`) to an active label:
   ```yaml
   TRACKER_LABEL: "status: waiting-for-triage"
   ```
2. Restart the app. You will see formatted logs for every detected issue:
   ```text
   ========================================
   [NEW FIRST-TIMERS ISSUE DETECTED]
   Issue: #40123 - Sample Issue Title
   URL: https://github.com/spring-projects/spring-boot/issues/40123
   Created: 2026-11-01T22:30:00Z
   ========================================
   ```
3. Restart it again with the same label. The application will recognize existing entries in the database and suppress duplicate alerts.

---

## Next Steps: Adding Slack Webhook Alerts

To forward new issue detections to Slack:

1. Set up an **Incoming Webhook** in your Slack workspace.
2. Add `SLACK_WEBHOOK_URL` to your environment configuration in `docker-compose.yml`.
3. In `IssueTrackerScheduler.java`, post the payload directly inside the `notify()` method:

```java
String webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
if (webhookUrl != null && !webhookUrl.isBlank()) {
    Map<String, String> payload = Map.of(
        "text", String.format("New Spring Boot issue found: <%s|#%d %s>",
                issue.getHtmlUrl(), issue.getNumber(), issue.getTitle())
    );
    webClient.post()
            .uri(webhookUrl)
            .bodyValue(payload)
            .retrieve()
            .toBodilessEntity()
            .subscribe();
}
```
