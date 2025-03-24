package com.todo.controller;

import com.todo.model.Task;
import com.todo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
    this.taskService = taskService;
}

@GetMapping
public List<Task> getTasks() {
    logger.info("GET /tasks called");
    return taskService.getAllTasks();
}

// deadline parametresi eklenmiş yeni addTask endpoint'i
@PostMapping
public Task addTask(@RequestParam String description, @RequestParam List<String> tags, @RequestParam String deadline) {
    LocalDate deadlineDate = LocalDate.parse(deadline); // "yyyy-MM-dd" formatında olmalı
    return taskService.addTask(description, tags, deadlineDate);
}

@PutMapping("/{id}/tags")
public Task updateTaskTags(@PathVariable Long id, @RequestParam List<String> tags) {
    return taskService.updateTaskTags(id, tags);
}

@PutMapping("/{id}")
public Task updateTask(@PathVariable Long id, @RequestParam String description) {
    Task task = taskService.getTaskById(id);
    if (task != null) {
        task.setDescription(description);
        return taskService.saveTask(task);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
}

@DeleteMapping("/{id}")
public void deleteTask(@PathVariable Long id) {
    taskService.deleteTask(id);
}

//@GetMapping("/filter")
//public List<Task> getTasksByTag(@RequestParam String tag) {
//    return taskService.getTasksByTag(tag);
//}

}
