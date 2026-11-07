package com.tracker.repository;

import com.tracker.model.TrackedIssue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TrackedIssueRepositoryTest {

    @Autowired
    private TrackedIssueRepository repository;

    @Test
    @DisplayName("Should persist and check existence of tracked issues by GitHub ID")
    void shouldSaveAndFindIssue() {
        TrackedIssue issue = new TrackedIssue(
                101L,
                45000L,
                "Sample first-timer task",
                "https://github.com/spring-projects/spring-boot/issues/45000",
                Instant.now()
        );

        repository.save(issue);

        assertThat(repository.existsById(101L)).isTrue();
        assertThat(repository.existsById(999L)).isFalse();

        Optional<TrackedIssue> found = repository.findById(101L);
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Sample first-timer task");
    }
}