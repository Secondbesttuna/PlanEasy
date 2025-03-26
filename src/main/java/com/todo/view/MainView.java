package com.todo.view;

import com.todo.model.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainView extends Application {
    private static final String API_URL = "http://localhost:8080/tasks";

    // Görevleri görüntülemek için
    private final ListView<Task> taskList = new ListView<>();

    // Sıralama durumu (false => Deadline, true => CreatedAt)
    private boolean sorting = false;
    private static boolean isLoggedIn = false;

    // Filtre metni
    private final TextField filterTagInput = new TextField();
    private Task selectedTaskBeforeRefresh;
    CheckBox showCompletedToggle = new CheckBox("Tamamlananları Göster");

    private String determineTaskColor(Task task) {
        if (task.getDeadline() == null || task.isCompleted()) {
            return "#CCCCCC";
        }

        LocalDate today = LocalDate.now();
        LocalDate deadline = task.getDeadline();
        long daysBetween = ChronoUnit.DAYS.between(today, deadline);

        if (daysBetween < 0) {
            return "#FF0000"; // Geçmiş tarihler için kırmızı
        } else if (daysBetween == 0) {
            return "#FF0000"; // Bugün için kırmızı
        } else if (daysBetween <= 7) {
            return "#FFFF00"; // 1 hafta içinde sarı
        } else {
            return "#00FF00"; // 1 haftadan fazla yeşil
        }
    }

    private void showLogIn(Stage primaryStage) {
        Button checkButton = new Button("Enter");
        VBox logIn = new VBox(10, checkButton);
        primaryStage.setScene(new Scene(logIn, 400, 600));
        primaryStage.show();
        checkButton.setOnAction(e -> {
            isLoggedIn = true;
            start(primaryStage);
        });
    }

    @Override
    public void start(Stage primaryStage) {

        Stage copy = primaryStage;
        if (!isLoggedIn) {
            showLogIn(primaryStage);
            startTimer();
        } else {
            primaryStage = copy;
            filterTagInput.setPromptText("Filtrelemek için etiket girin");

            // Toggle benzeri bir switch için CheckBox kullanıyoruz
            CheckBox toggleCheckBox = new CheckBox();
            Label lblDeadline = new Label("Deadline");
            Label lblCreatedAt = new Label("Created At");

            // Başlangıçta Deadline sıralaması olsun
            toggleCheckBox.setSelected(false);

            // Toggle davranışı: seçiliyse CreatedAt, seçili değilse Deadline
            toggleCheckBox.setOnAction(e -> {
                sorting = toggleCheckBox.isSelected();
                refles();
            });

            showCompletedToggle.setOnAction(e -> refles());

            // Bu HBox'ta solda "Deadline" yazısı, ortada checkbox, sağda "Created At"
            // yazısı var
            HBox toggleContainer = new HBox(10, lblDeadline, toggleCheckBox, lblCreatedAt);

            // İşlem butonları
            Button addButton = new Button("Add Task");
            Button updateButton = new Button("Update Task");
            Button deleteButton = new Button("Delete Task");
            Button markButton = new Button("Complete Task");

            // Add Task: ayrı pencerede yeni görev ekleme
            addButton.setOnAction(e -> openAddTaskWindow(taskList));

            // Update Task: ayrı pencerede seçili görevi düzenleme
            updateButton.setOnAction(e -> {
                Task selectedTask = taskList.getSelectionModel().getSelectedItem();
                if (selectedTask != null) {
                    openUpdateTaskWindow(selectedTask, taskList);
                }
            });

            // Delete Task: seçili görevi sil
            deleteButton.setOnAction(e -> {
                Task selectedTask = taskList.getSelectionModel().getSelectedItem();
                if (selectedTask != null) {
                    deleteTask(selectedTask);
                    refles();
                }
            });

            markButton.setOnAction(e -> {
                Task selectedTask = taskList.getSelectionModel().getSelectedItem();
                if (selectedTask != null) {
                    markTaskAsCompleted(selectedTask);
                    refles();
                }
            });

            // Ana layout
            // Butonları layout'a ekle
            VBox layout = new VBox(10,
                    filterTagInput,
                    toggleContainer,
                    showCompletedToggle, // Yeni toggle
                    taskList,
                    addButton, updateButton, deleteButton, markButton);
            primaryStage.setScene(new Scene(layout, 400, 600));
            primaryStage.setTitle("To-Do List with Tags");
            primaryStage.show();

            // Başlangıçta görevleri çek
            refles();

            // Otomatik yenileme (1 saniyede bir)
            startTimer();
        }
    }

    /**
     * 1 saniyede bir çalışan Timer fonksiyonu.
     * Filtre ve sıralama durumuna göre listeyi yeniler.
     */
    public void startTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            refles();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    /**
     * Filtre ve sıralama durumunu göz önünde bulundurarak listeyi yenileyen metod.
     */
    public void refles() {
        selectedTaskBeforeRefresh = taskList.getSelectionModel().getSelectedItem();

        String filterTag = filterTagInput.getText();

        RestTemplate restTemplate = new RestTemplate();
        Task[] tasks = restTemplate.getForObject(API_URL, Task[].class);
        if (tasks.length != 0) {
            List<Task> coplate_filteredTasks = Arrays.stream(tasks)
                    .filter(task -> showCompletedToggle.isSelected() == task.isCompleted())
                    .collect(Collectors.toList());

            if (!filterTag.isEmpty()) {
                // Filtre uygulanacak
                if (coplate_filteredTasks != null) {
                    List<Task> filteredTasks = coplate_filteredTasks.stream()
                            .filter(task -> task.getTags() != null && task.getTags().contains(filterTag)
                                    && (showCompletedToggle.isSelected() == task.isCompleted()))
                            .collect(Collectors.toList());
                    // Deadline'a göre bir ilk sıralama (isterseniz direk sorting parametresine göre
                    // de ayarlayabilirsiniz)
                    filteredTasks.sort(Comparator.comparing(
                            Task::getDeadline,
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    taskList.getItems().setAll(filteredTasks);
                }
            } else {
                taskList.getItems().setAll(coplate_filteredTasks);
            }
        }

        // Sıralama durumu (sorting = true => createdAt, false => deadline)
        if (sorting) {
            sortTasksByCreatedAt(taskList);
        } else {
            sortTasksByDeadline(taskList);
        }

        if (selectedTaskBeforeRefresh != null) {
            taskList.getItems().stream()
                    .filter(task -> task.getId().equals(selectedTaskBeforeRefresh.getId()))
                    .findFirst()
                    .ifPresent(task -> {
                        taskList.getSelectionModel().select(task);
                        taskList.scrollTo(task);
                    });
        }

        taskList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String deadlineStr = task.getDeadline() != null ? task.getDeadline().toString() : "Belirtilmemiş";
                    setText(task.getDescription() + " - Deadline: " + deadlineStr);

                    String color = determineTaskColor(task);
                    setStyle("-fx-background-color: " + color + ";" +
                            "-fx-border-color: derive(" + color + ", -30%);" +
                            "-fx-text-fill: " + getContrastColor(color) + ";" +
                            "-fx-padding: 10;");
                    if (selectedTaskBeforeRefresh != null && task.equals(selectedTaskBeforeRefresh)) {
                        taskList.getSelectionModel().select(task);
                    }
                }
            }
        });
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
                    refles();
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

    // "Update Task" için ayrı pencere (güncelleme formu: açıklama, etiket,
    // deadline)
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
                refles();
                updateStage.close();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Lütfen geçerli bir tarih girin (sayı formatında).");
                alert.showAndWait();
            }
        });

        VBox updateLayout = new VBox(10, updateTaskInput, updateTagInput, updateMonthInput, updateDayInput,
                updateYearInput, saveUpdateButton);
        updateStage.setScene(new Scene(updateLayout, 300, 250));
        updateStage.show();
    }

    // Görevi full update yapan metod (description, tags, deadline)
    private void updateTaskFull(Task task, String description, String tags, LocalDate deadline) {
        RestTemplate restTemplate = new RestTemplate();
        String url = API_URL + "/" + task.getId() + "/updateFull?description=" + description + "&tags=" + tags
                + "&deadline=" + deadline.toString();
        restTemplate.put(url, null);
    }

    // Yeni görev ekleme metodu (deadline parametresiyle)
    private void addTask(String description, String tags, LocalDate deadline) {
        RestTemplate restTemplate = new RestTemplate();
        String url = API_URL + "?description=" + description + "&tags=" + tags + "&deadline=" + deadline.toString();
        restTemplate.postForObject(url, null, Task.class);
    }

    private String getContrastColor(String hexColor) {
        hexColor = hexColor.replace("#", "");
        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        return luminance > 0.5 ? "black" : "white";
    }

    // Bir görevi veritabanından siler
    private void deleteTask(Task task) {
        if (selectedTaskBeforeRefresh != null && task.equals(selectedTaskBeforeRefresh)) {
            selectedTaskBeforeRefresh = null;
        }
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.delete(API_URL + "/" + task.getId());
    }

    private void markTaskAsCompleted(Task task) {
        RestTemplate restTemplate = new RestTemplate();
        String url = API_URL + "/" + task.getId() + "/complete";
        restTemplate.put(url, null);
    }

    // Frontend sıralama: Deadline'a göre
    private void sortTasksByDeadline(ListView<Task> taskList) {
        List<Task> currentTasks = new ArrayList<>(taskList.getItems());
        currentTasks.sort(Comparator.comparing(
                Task::getDeadline,
                Comparator.nullsLast(Comparator.naturalOrder())));
        taskList.getItems().setAll(currentTasks);
    }

    // Frontend sıralama: Eklenme tarihine (createdAt) göre
    private void sortTasksByCreatedAt(ListView<Task> taskList) {
        List<Task> currentTasks = new ArrayList<>(taskList.getItems());
        currentTasks.sort(Comparator.comparing(
                Task::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        taskList.getItems().setAll(currentTasks);
    }

}
