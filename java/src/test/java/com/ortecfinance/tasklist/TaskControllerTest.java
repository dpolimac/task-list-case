package com.ortecfinance.tasklist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    private MockMvc mockMvc;
    private TaskList taskList;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        taskList = new TaskList();
        mockMvc = MockMvcBuilders.standaloneSetup(new TaskController(taskList)).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /projects: creates project on valid name -> 200 OK")
    void createProject_ok() throws Exception {
        var body = objectMapper.writeValueAsString(new TaskController.CreateProjectRequest("P"));
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // verify in memory
        Map<String, List<Task>> projects = taskList.getAllProjects();
        assertTrue(projects.containsKey("P"));
    }

    @Test
    @DisplayName("POST /projects: empty or missing name -> 400 Bad Request")
    void createProject_badRequest() throws Exception {
        // null name
        var bodyNull = objectMapper.writeValueAsString(new TaskController.CreateProjectRequest(null));
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyNull))
                .andExpect(status().isBadRequest());

        // blank name
        var bodyBlank = objectMapper.writeValueAsString(new TaskController.CreateProjectRequest("  "));
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyBlank))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /projects: returns all projects map")
    void getProjects_returnsMap() throws Exception {
        taskList.addProject("A");
        taskList.addTask("A", "t1");

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.A").isArray())
                .andExpect(jsonPath("$.A[0].description").value("t1"));
    }

    @Test
    @DisplayName("POST /projects/{id}/tasks: creates task -> 201 Created")
    void createTask_created() throws Exception {
        taskList.addProject("P");
        var body = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("t1"));

        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        assertEquals(1, taskList.getAllProjects().get("P").size());
        assertEquals("t1", taskList.getAllProjects().get("P").getFirst().getDescription());
    }

    @Test
    @DisplayName("POST /projects/{id}/tasks: unknown project -> 404 Not Found")
    void createTask_unknownProject() throws Exception {
        var body = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("t1"));
        mockMvc.perform(post("/projects/UNKNOWN/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /projects/{id}/tasks: invalid request body -> 400 Bad Request")
    void createTask_badRequest() throws Exception {
        taskList.addProject("P");

        // null description
        var nullDesc = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest(null));
        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullDesc))
                .andExpect(status().isBadRequest());

        // blank description
        var blankDesc = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("  "));
        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankDesc))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /projects/{id}/tasks/{taskId}?deadline=dd-MM-yyyy: valid -> 201 Created")
    void updateDeadline_created() throws Exception {
        taskList.addProject("P");
        var body = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("t1"));

        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/P/tasks/1Xdeadline")
                        .param("deadline", "01-01-2024"))
                .andExpect(status().isCreated());

        var task = taskList.getAllProjects().get("P").get(0);
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MM-yyyy");
        assertEquals("01-01-2024", fmt.format(task.getDeadline()));
    }

    @Test
    @DisplayName("PUT /projects/{id}/tasks/{taskId}?deadline=dd-MM-yyyy: non-existent id -> 404 Not Found")
    void updateDeadline_non_existent_id() throws Exception {
        taskList.addProject("P");
        var body = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("t1"));

        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/P/tasks/55Xdeadline")
                        .param("deadline", "01-01-2024"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /projects/{id}/tasks/{taskId}?deadline=dd-MM-yyyy: invalid date -> 400 Bad Request")
    void updateDeadline_bad_date() throws Exception {
        taskList.addProject("P");
        var body = objectMapper.writeValueAsString(new TaskController.CreateTaskRequest("t1"));

        mockMvc.perform(post("/projects/P/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/projects/P/tasks/1Xdeadline")
                        .param("deadline", "2024-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /projects/{id}/tasks/{taskId}?deadline=...: unknown task -> 404 Not Found")
    void updateDeadline_unknownTask() throws Exception {
        taskList.addProject("P");

        mockMvc.perform(put("/projects/P/tasks/999")
                        .param("deadline", "01-01-2024"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /projects/view_by_deadline: returns deadlines and noDeadlineTasks")
    void viewByDeadline_returnsAggregations() throws Exception {
        taskList.addProject("P");
        taskList.addTask("P", "t1"); // id=1

        mockMvc.perform(get("/projects/view_by_deadline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deadlines").exists())
                .andExpect(jsonPath("$.noDeadlineTasks.P").isArray())
                .andExpect(jsonPath("$.noDeadlineTasks.P[0].description").value("t1"));
    }
}