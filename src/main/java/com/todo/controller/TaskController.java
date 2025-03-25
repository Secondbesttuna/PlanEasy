package com.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.model.Task;
import com.todo.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;

import java.io.File;

import java.time.LocalDate;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskService taskService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<Task> getTasks() {
        logger.info("GET /tasks called");
        return taskService.getAllTasks();
    }

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

    @GetMapping("/list")
    @ResponseBody
    public String tasksList() {
        List<Task> tasks = taskService.getAllTasks();
        StringBuilder html = new StringBuilder("<html><body>");
        html.append("<h2>Görev Listesi</h2>");
        html.append("<ul>");
        for(Task t : tasks) {
            html.append("<li>")
                .append(t.getDescription())
                .append(" - Deadline: ")
                .append(t.getDeadline())
                .append("</li>");
        }
        html.append("</ul>");
        // Sayfayı her 3 saniyede bir otomatik yenile
        html.append("<script>setTimeout(function(){ window.location.reload(); }, 3000);</script>");
        html.append("</body></html>");
        return html.toString();
    }

    // JSON dışarı aktarma
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportTasks() {
        try {
            // Tüm görevleri al
            List<Task> tasks = taskService.getAllTasks();
            // Görev listesini JSON string'e çevir
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(tasks);
            // JSON string'i bayt dizisine çevir
            ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=tasks.json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(resource.contentLength())
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/exportToFile")
    public ResponseEntity<String> exportTasksToFile() {
        try {
            List<Task> tasks = taskService.getAllTasks();
            ObjectMapper mapper = new ObjectMapper();
            // tasks.json dosyasına yaz
            mapper.writeValue(new File("tasks.json"), tasks);
            return ResponseEntity.ok("Görevler tasks.json dosyasına yazıldı.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return ResponseEntity.status(500).body("Dosya yazılırken hata oluştu.");
        }
    }

    @GetMapping("/importFromFile")
    @ResponseBody
    public ResponseEntity<String> handleImportFileGet() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body("GET metodu desteklenmiyor. Lütfen import formunu kullanın: /tasks/importForm");
    }

    @PostMapping(value = "/importFromFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importTasksFromFile(@RequestParam("file") MultipartFile file) {
        try {
            // Önce mevcut tüm görevleri temizle
            taskService.deleteAllTasks();

            // Dosya içeriğini oku ve String'e çevir
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            // JSON içeriğini Task listesine parse et
            List<Task> importedTasks = objectMapper.readValue(jsonContent, new TypeReference<List<Task>>() {});

            // Her bir görevi kaydet
            for (Task t : importedTasks) {
                Task newTask = new Task(t.getDescription(), t.getTags(), t.getDeadline());
                taskService.saveTask(newTask);
            }
            return ResponseEntity.ok("Görevler başarıyla içeri aktarıldı!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Import işlemi sırasında hata oluştu: " + e.getMessage());
        }
    }


    @GetMapping("/importForm")
    @ResponseBody
    public String importForm() {
        return "<html>" +
        "<body>" +
        "<h2>Dosya Üzerinden Import İşlemi</h2>" +
        "<form method='POST' action='/tasks/importFromFile' enctype='multipart/form-data'>" +
        "Dosya Seçin: <input type='file' name='file' /> <br/><br/>" +
        "<input type='submit' value='Import Et' />" +
        "</form>" +
        "</body>" +
        "</html>";
    }


    // JSON içeri aktarma: Gelen verideki tüm görevleri eklemeden önce mevcut görevler silinir.
    // @PostMapping("/import")
    // public ResponseEntity<String> importTasks(@RequestBody List<Task> importedTasks) {
    //     // Mevcut tüm görevleri sil
    //     taskService.deleteAllTasks();

    //     // Her bir JSON'dan gelen görevi yeni bir Task nesnesi olarak kaydet
    //     for (Task t : importedTasks) {
    //         // Yeni Task oluşturulurken, id ve oluşturulma tarihinin otomatik ayarlanması için
    //         Task newTask = new Task(t.getDescription(), t.getTags(), t.getDeadline());
    //         taskService.saveTask(newTask);
    //     }
    //     return ResponseEntity.ok("Import işlemi başarıyla tamamlandı.");
    // }


    //@GetMapping("/filter")
    //public List<Task> getTasksByTag(@RequestParam String tag) {
    //    return taskService.getTasksByTag(tag);
    //}

}
