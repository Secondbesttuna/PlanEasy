package com.todo.repository;

import com.todo.model.Task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    //@Query("SELECT t FROM Task t WHERE :tag MEMBER OF t.tags")
    //List<Task> findByTag(@Param("tag") String tag);
    List<Task> findByCompleted(boolean completed);
}
