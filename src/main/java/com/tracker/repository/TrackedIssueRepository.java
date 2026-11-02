package com.tracker.repository;

import com.tracker.model.TrackedIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackedIssueRepository extends JpaRepository<TrackedIssue, Long> {
}
