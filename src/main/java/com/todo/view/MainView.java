package com.todo.view;

import com.todo.model.Task;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainView extends Application {
    private static final String API_URL = "http://localhost:8080/tasks";

    
    @Override
public void start(Stage primaryStage) {
    // Filtreleme kontrolleri
    TextField filterTagInput = new TextField();
    filterTagInput.setPromptText("Filtrelemek için etiket girin");
    Button filterButton = new Button("Filter Tasks");
    Button clearFilterButton = new Button("Clear Filter");

    ListView<Task> taskList = new ListView<>();

    // Update alanları (ana pencerede kalıyor)
    TextField taskInput = new TextField();
    taskInput.setPromptText("Güncellenecek görev açıklaması");
    TextField tagInput = new TextField();
    tagInput.setPromptText("Güncellenecek etiket(ler)");

    Button addButton = new Button("Add Task");
    Button deleteButton = new Button("Delete Task");
    Button updateButton = new Button("Update Task");
    Button addTagButton = new Button("Update Tags");

    // Mevcut görevleri sunucudan çek
    fetchTasks(taskList);

    // Filtreleme butonlarının aksiyonu
    filterButton.setOnAction(e -> {
        String filterTag = filterTagInput.getText();
        if (!filterTag.isEmpty()) {
            RestTemplate restTemplate = new RestTemplate();
            Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
            if (tasks != null) {
                List<Task> filteredTasks = Arrays.stream(tasks)
                        .filter(task -> task.getTags() != null && task.getTags().contains(filterTag))
                        .collect(Collectors.toList());
                taskList.getItems().setAll(filteredTasks);
            }
        }
    });
    
    clearFilterButton.setOnAction(e -> {
        fetchTasks(taskList);
        filterTagInput.clear();
    });

    // "Add Task" butonuna basıldığında ayrı pencere açılıyor
    addButton.setOnAction(e -> openAddTaskWindow(taskList));

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

    VBox layout = new VBox(10, filterTagInput, filterButton, clearFilterButton, taskList, addButton, taskInput, tagInput, updateButton, addTagButton, deleteButton);
    primaryStage.setScene(new Scene(layout, 400, 550));
    primaryStage.setTitle("To-Do List with Tags");
    primaryStage.show();
}

// Yeni pencere: Add Task penceresinde deadline için ek alanlar eklendi
private void openAddTaskWindow(ListView<Task> taskList) {
    Stage addStage = new Stage();
    addStage.setTitle("Add New Task");

    TextField newTaskInput = new TextField();
    newTaskInput.setPromptText("Görev Açıklaması");
    TextField newTagInput = new TextField();
    newTagInput.setPromptText("Etiket(ler)");

    // Deadline için ayrı alanlar: Ay, Gün, Yıl
    TextField monthInput = new TextField();
    monthInput.setPromptText("Ay (MM)");
    TextField dayInput = new TextField();
    dayInput.setPromptText("Gün (DD)");
    TextField yearInput = new TextField();
    yearInput.setPromptText("Yıl (YYYY)");

    Button saveButton = new Button("Kaydet");
    saveButton.setOnAction(e -> {
        String description = newTaskInput.getText();
        String tags = newTagInput.getText();
        try {
            int month = Integer.parseInt(monthInput.getText());
            int day = Integer.parseInt(dayInput.getText());
            int year = Integer.parseInt(yearInput.getText());
            LocalDate deadline = LocalDate.of(year, month, day);
            if (!description.isEmpty()) {
                addTask(description, tags, deadline);
                fetchTasks(taskList);
                addStage.close();
            }
        } catch(NumberFormatException ex) {
            // Hatalı sayı girilirse kullanıcıya hata mesajı gösterebilirsiniz
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lütfen geçerli bir tarih girin (sayı formatında).");
            alert.showAndWait();
        }
    });

    VBox addLayout = new VBox(10, newTaskInput, newTagInput, monthInput, dayInput, yearInput, saveButton);
    addStage.setScene(new Scene(addLayout, 300, 250));
    addStage.show();
}

// deadline parametresi eklenmiş yeni addTask metodu
private void addTask(String description, String tags, LocalDate deadline) {
    RestTemplate restTemplate = new RestTemplate();
    String url = API_URL + "?description=" + description + "&tags=" + tags + "&deadline=" + deadline.toString();
    restTemplate.postForObject(url, null, Task.class);
}

private void fetchTasks(ListView<Task> taskList) {
    RestTemplate restTemplate = new RestTemplate();
    Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
    if (tasks != null) {
        List<Task> taskListData = Arrays.asList(tasks);
        taskList.getItems().setAll(taskListData);
    }
    
    taskList.setCellFactory(param -> new ListCell<>() {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setText(null);
            } else {
                String deadlineStr = task.getDeadline() != null ? task.getDeadline().toString() : "N/A";
                setText(task.getDescription() + " - " + task.getCreatedAt() + " | Deadline: " + deadlineStr + " | Tags: " + task.getTags());
            }
        }
    });
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
