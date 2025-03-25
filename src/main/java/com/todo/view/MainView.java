package com.todo.view;

import com.todo.model.Task;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

        // Görev listesini gösterecek ListView
        ListView<Task> taskList = new ListView<>();

        // İşlem butonları
        Button addButton = new Button("Add Task");
        Button updateButton = new Button("Update Task");
        Button deleteButton = new Button("Delete Task");

        // Sıralama butonları
        Button sortByDeadlineButton = new Button("Sort By Deadline");
        sortByDeadlineButton.setOnAction(e -> sortTasksByDeadline(taskList));

        Button sortByCreatedAtButton = new Button("Sort By Created At");
        sortByCreatedAtButton.setOnAction(e -> sortTasksByCreatedAt(taskList));

        // Filtreleme aksiyonları
        filterButton.setOnAction(e -> {
            String filterTag = filterTagInput.getText();
            if (!filterTag.isEmpty()) {
                RestTemplate restTemplate = new RestTemplate();
                Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
                if (tasks != null) {
                    List<Task> filteredTasks = Arrays.stream(tasks)
                            .filter(task -> task.getTags() != null && task.getTags().contains(filterTag))
                            .collect(Collectors.toList());
                    filteredTasks.sort(Comparator.comparing(
                            Task::getDeadline,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ));
                    taskList.getItems().setAll(filteredTasks);
                }
            }
        });

        clearFilterButton.setOnAction(e -> {
            fetchTasks(taskList);
            filterTagInput.clear();
        });

        // "Add Task" butonu ayrı pencere açar
        addButton.setOnAction(e -> openAddTaskWindow(taskList));

        // "Update Task" butonu da ayrı pencere açarak seçili görevin tüm alanlarını düzenlemeye olanak tanır
        updateButton.setOnAction(e -> {
            Task selectedTask = taskList.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                openUpdateTaskWindow(selectedTask, taskList);
            }
        });

        deleteButton.setOnAction(e -> {
            Task selectedTask = taskList.getSelectionModel().getSelectedItem();
            if (selectedTask != null) {
                deleteTask(selectedTask);
                fetchTasks(taskList);
            }
        });

        VBox layout = new VBox(10,
                filterTagInput, filterButton, clearFilterButton,
                sortByDeadlineButton, sortByCreatedAtButton,
                taskList, addButton, updateButton, deleteButton);
        primaryStage.setScene(new Scene(layout, 400, 600));
        primaryStage.setTitle("To-Do List with Tags");
        primaryStage.show();

        fetchTasks(taskList);
    }

    // "Add Task" için ayrı pencere (deadline, etiket vs. ekli hali)
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
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lütfen geçerli bir tarih girin (sayı formatında).");
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Geçerli bir tarih girin.");
                alert.showAndWait();
            }
        });

        VBox addLayout = new VBox(10, newTaskInput, newTagInput, monthInput, dayInput, yearInput, saveButton);
        addStage.setScene(new Scene(addLayout, 300, 250));
        addStage.show();
    }

    // "Update Task" için ayrı pencere (güncelleme formu: açıklama, etiket, deadline)
    private void openUpdateTaskWindow(Task task, ListView<Task> taskList) {
        Stage updateStage = new Stage();
        updateStage.setTitle("Update Task");

        // Seçili görevin verileriyle ön doldurma
        TextField updateTaskInput = new TextField(task.getDescription());
        updateTaskInput.setPromptText("Görev Açıklaması");

        String tagsStr = (task.getTags() != null) ? String.join(",", task.getTags()) : "";
        TextField updateTagInput = new TextField(tagsStr);
        updateTagInput.setPromptText("Etiket(ler)");

        // Deadline alanları
        TextField updateMonthInput = new TextField();
        TextField updateDayInput = new TextField();
        TextField updateYearInput = new TextField();
        if (task.getDeadline() != null) {
            updateMonthInput.setText(String.format("%02d", task.getDeadline().getMonthValue()));
            updateDayInput.setText(String.format("%02d", task.getDeadline().getDayOfMonth()));
            updateYearInput.setText(String.valueOf(task.getDeadline().getYear()));
        }
        updateMonthInput.setPromptText("Ay (MM)");
        updateDayInput.setPromptText("Gün (DD)");
        updateYearInput.setPromptText("Yıl (YYYY)");

        Button saveUpdateButton = new Button("Güncelle");
        saveUpdateButton.setOnAction(e -> {
            String newDescription = updateTaskInput.getText();
            String newTags = updateTagInput.getText();
            try {
                int month = Integer.parseInt(updateMonthInput.getText());
                int day = Integer.parseInt(updateDayInput.getText());
                int year = Integer.parseInt(updateYearInput.getText());
                LocalDate newDeadline = LocalDate.of(year, month, day);
                updateTaskFull(task, newDescription, newTags, newDeadline);
                fetchTasks(taskList);
                updateStage.close();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lütfen geçerli bir tarih girin (sayı formatında).");
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Geçerli bir tarih girin.");
                alert.showAndWait();
            }
        });

        VBox updateLayout = new VBox(10, updateTaskInput, updateTagInput, updateMonthInput, updateDayInput, updateYearInput, saveUpdateButton);
        updateStage.setScene(new Scene(updateLayout, 300, 250));
        updateStage.show();
    }

    // Görevi full update yapan metod (description, tags, deadline)
    private void updateTaskFull(Task task, String description, String tags, LocalDate deadline) {
        RestTemplate restTemplate = new RestTemplate();
        // Backend'de /updateFull endpoint'ini desteklediğinizi varsayıyoruz.
        String url = API_URL + "/" + task.getId() + "/updateFull?description=" + description 
                + "&tags=" + tags + "&deadline=" + deadline.toString();
        restTemplate.put(url, null);
    }

    // Yeni görev ekleme metodunda deadline parametresi
    private void addTask(String description, String tags, LocalDate deadline) {
        RestTemplate restTemplate = new RestTemplate();
        String url = API_URL + "?description=" + description + "&tags=" + tags + "&deadline=" + deadline.toString();
        restTemplate.postForObject(url, null, Task.class);
    }

    // Backend'den görevleri çekip deadline'a göre sıralayarak ListView'e ekleyen metod
    private void fetchTasks(ListView<Task> taskList) {
        RestTemplate restTemplate = new RestTemplate();
        Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
        if (tasks != null) {
            List<Task> taskListData = Arrays.asList(tasks);
            taskListData.sort(Comparator.comparing(
                    Task::getDeadline,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
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
                    setText(task.getDescription() + " - " + task.getCreatedAt()
                            + " | Deadline: " + deadlineStr + " | Tags: " + task.getTags());
                }
            }
        });
    }

    private void deleteTask(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(API_URL + "/" + task.getId());
    }

    // Frontend sıralama: deadline'a göre
    private void sortTasksByDeadline(ListView<Task> taskList) {
        List<Task> currentTasks = new ArrayList<>(taskList.getItems());
        currentTasks.sort(Comparator.comparing(
                Task::getDeadline,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        taskList.getItems().setAll(currentTasks);
    }

    // Frontend sıralama: eklenme tarihine (createdAt) göre
    private void sortTasksByCreatedAt(ListView<Task> taskList) {
        List<Task> currentTasks = new ArrayList<>(taskList.getItems());
        currentTasks.sort(Comparator.comparing(
                Task::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        taskList.getItems().setAll(currentTasks);
    }
}
