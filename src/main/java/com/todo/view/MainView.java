package com.todo.view;

import com.todo.model.Task;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class MainView extends Application {
    private static final String API_URL = "http://localhost:8080/tasks";

    @Override
    public void start(Stage primaryStage) {
        ListView<Task> taskList = new ListView<>();
        TextField taskInput = new TextField();
        TextField tagInput = new TextField();
        Button addButton = new Button("Add Task");
        Button deleteButton = new Button("Delete Task");
        Button updateButton = new Button("Update Task");
        Button addTagButton = new Button("Update Tags");

        fetchTasks(taskList);

        addButton.setOnAction(e -> {
            String task = taskInput.getText(); // description input
            String tags = tagInput.getText(); // tags input
            if (!task.isEmpty()) {
                addTask(task, tags);
                fetchTasks(taskList);
                taskInput.clear();
                tagInput.clear();
            }
        });

        deleteButton.setOnAction(e -> {
            Task selectedTask = taskList.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                deleteTask(selectedTask);
                fetchTasks(taskList);
            }
        });

        updateButton.setOnAction(e -> {
            Task selectedTask = taskList.getSelectionModel().getSelectedItem();
            String newDescription = taskInput.getText();
            if (selectedTask != null && !newDescription.isEmpty()) {
                updateTask(selectedTask, newDescription);
                fetchTasks(taskList);
                taskInput.clear();
            }
        });

        addTagButton.setOnAction(e -> {
            Task selectedTask = taskList.getSelectionModel().getSelectedItem();
            String newTags = tagInput.getText();
            if (selectedTask != null && !newTags.isEmpty()) {
                updateTags(selectedTask, newTags);
                fetchTasks(taskList);
                tagInput.clear();
            }
        });

        VBox layout = new VBox(10, taskList, taskInput, tagInput, addButton, deleteButton, updateButton, addTagButton);
        primaryStage.setScene(new Scene(layout, 400, 500));
        primaryStage.setTitle("To-Do List with Tags");
        primaryStage.show();
    }

    private void fetchTasks(ListView<Task> taskList) {
        RestTemplate restTemplate = new RestTemplate();
        Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
        if (tasks != null) {
            List<Task> taskListData = Arrays.asList(tasks);
            taskList.getItems().setAll(taskListData);
        }
        
        // ListView'de açıklama, tarih ve etiketleri göstermek için
        taskList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                } else {
                    setText(task.getDescription() + " - " + task.getCreatedAt() + " | Tags: " + task.getTags());
                }
            }
        });
    }

    private void addTask(String description, String tags) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(API_URL + "?description=" + description + "&tags=" + tags, null, Task.class);
    }

    private void deleteTask(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(API_URL + "/" + task.getId());
    }

    private void updateTask(Task task, String newDescription) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(API_URL + "/" + task.getId() + "?description=" + newDescription, null);
    }

    private void updateTags(Task task, String newTags) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.put(API_URL + "/" + task.getId() + "/tags?tags=" + newTags, null);
    }
}
