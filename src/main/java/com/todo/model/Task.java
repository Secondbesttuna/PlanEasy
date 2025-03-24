package com.todo.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String description;
    private boolean completed;
    private LocalDateTime createdAt;

    @ElementCollection
    private List<String> tags;

    private LocalDate deadline; // deadline alanı eklendi  

public Task() {
    this.createdAt = LocalDateTime.now();
}

public Task(String description, List<String> tags) {
    this.description = description;
    this.completed = false;
    this.createdAt = LocalDateTime.now();
    this.tags = tags;
}

// Yeni constructor deadline alanı eklendi
public Task(String description, List<String> tags, LocalDate deadline) {
    this.description = description;
    this.completed = false;
    this.createdAt = LocalDateTime.now();
    this.tags = tags;
    this.deadline = deadline;
}

public Long getId() { return id; }
public String getDescription() { return description; }
public boolean isCompleted() { return completed; }
public LocalDateTime getCreatedAt() { return createdAt; }
public List<String> getTags() { return tags; }
public LocalDate getDeadline() { return deadline; } // getter için

public void setDescription(String description) { this.description = description; }
public void setCompleted(boolean completed) { this.completed = completed; }
public void setTags(List<String> tags) { this.tags = tags; }
public void setDeadline(LocalDate deadline) { this.deadline = deadline; } // setter için
}
