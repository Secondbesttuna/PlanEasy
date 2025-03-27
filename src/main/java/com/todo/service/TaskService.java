package com.todo.service;

import com.todo.model.Task;
import com.todo.repository.TaskRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;



@Service
public class TaskService {
    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task addTask(String description, List<String> tags, LocalDate deadline) {
        return taskRepository.save(new Task(description, tags, deadline));
    }

    public Task updateTaskTags(Long id, List<String> newTags) {
        Task task = getTaskById(id);
        if (task != null) {
            task.setTags(newTags);
            return taskRepository.save(task);
        }
        return null;
    }

    public Task updateTaskFull(Long id, String description, List<String> tags, LocalDate deadline) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new RuntimeException("Task bulunamadı!"));
        task.setDescription(description);
        task.setTags(tags);
        task.setDeadline(deadline);
        return taskRepository.save(task); // Direkt veritabanını günceller
    }

    public void markTaskAsCompleted(Long taskId) {
        Optional<Task> task = taskRepository.findById(taskId);
        task.ifPresent(t -> {
            t.setCompleted(true);
            taskRepository.save(t);
        });
    }

    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }

    public void deleteAllTasks() {
        taskRepository.deleteAll();
    }

    //public List<Task> getTasksByTag(String tag) {
    //    return taskRepository.findByTag(tag);
    //}

}
