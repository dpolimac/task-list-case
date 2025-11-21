package com.ortecfinance.tasklist;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
public class TaskController {

    private final TaskList taskList;
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    public TaskController(TaskList taskList) {
        this.taskList = taskList;
        formatter.setLenient(false);
    }

    // Endpoints

    @PostMapping
    public ResponseEntity<Void> createProject(@RequestBody CreateProjectRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        taskList.addProject(request.name());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public Map<String, List<Task>> getProjects() {
        return taskList.getAllProjects();
    }

    @PostMapping("/{project_id}/tasks")
    public ResponseEntity<Void> createTask(@PathVariable String project_id,
                                           @RequestBody CreateTaskRequest request) {
        if (request == null || request.description() == null || request.description().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean added = taskList.addTask(project_id, request.description());

        if (!added) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{project_id}/tasks/{task_id}?deadline")
    public ResponseEntity<Void> updateDeadline(@PathVariable String project_id,
                                           @PathVariable int task_id,
                                           @RequestParam String deadline) {
        Date date;
        try {
            date = formatter.parse(deadline);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        boolean added = taskList.addDeadline(task_id, date);
        if (!added) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
   }

   @GetMapping("/view_by_deadline")
   public ViewByDeadlineResponse getTasksByDeadline() {
        Map<Date, Map<String, List<Task>>> tasksByDeadline = taskList.getTasksByDeadline();
        Map<String, List<Task>> noDeadlineTasks = taskList.getTasksWithoutDeadline();
        return new ViewByDeadlineResponse(tasksByDeadline, noDeadlineTasks);
    }

    // DTOs

    public record CreateProjectRequest(String name) { }

    public record CreateTaskRequest(String description) { }

    public record ViewByDeadlineResponse(
            Map<Date, Map<String, List<Task>>> deadlines,
            Map<String, List<Task>> noDeadlineTasks
    ) { }

}
