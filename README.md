📌 PlanEasy - Task Management App
A simple and efficient to-do list application designed for seamless task management.

📖 Project Overview
PlanEasy is a task management application that allows users to efficiently organize their daily activities. Unlike traditional to-do list apps, PlanEasy operates independently without external APIs, ensuring data security and seamless offline access.

🚀 Features:
✅ Add, edit, and delete tasks.
✅ Categorize tasks and filter by priority or due date.
✅ Enable reminders for upcoming tasks.
✅ Securely store and backup task data.
✅ Cross-platform support (Android, and potential web version).

🛠️ Technology Stack
Frontend: Java (javaFX)
Backend: H2/spring-boot
Version Control: GitHub


Build:
mvn clean install

Run:
for server: mvn spring-boot:run
for GUI: mvn javafx:run

/PlanEasy
 ├── /src
 |   ├── /docxs
 │   ├── /main/java/com/todo
 │                     ├── /controler
 │                         └── TaskControler.java
 │                     ├── /model
 │                         └── Task.java
 │                     ├── /repository
 │                         └── TaskRepository.java
 │                     ├── /service
 │                         └── TaskService.java
 │                     ├── /view
 │                         └── MainView.java
 │                     └── TodoListApplication.java
 ├── README.md
 └── pom.xml

