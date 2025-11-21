package com.ortecfinance.tasklist;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class TaskList {

    private final Map<String, List<Task>> projects;
    private long lastId = 0;

    public TaskList() {
        this.projects = new LinkedHashMap<>();
    }


    /**
     * Adds or updates the deadline for a task with the specified ID.
     *
     * @param id the unique identifier of the task to be updated
     * @param deadline the new deadline to be set for the task
     * @return true if the task is found and the deadline is successfully updated,
     *         false if no task with the specified ID exists
     */
    public boolean addDeadline(int id, Date deadline) {
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDeadline(deadline);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves all projects and their associated tasks.
     *
     * @return an immutable map where the key is the project name (as a string) and the value is
     *         a list of tasks associated with the project.
     */
    public Map<String, List<Task>> getAllProjects() {
        Map<String, List<Task>> allProjects = new LinkedHashMap<>(projects);
        return Collections.unmodifiableMap(allProjects);
    }

    /**
     * Retrieves a map of consisting only of tasks with deadlines
     * matching today's date grouped by their associated project.
     *
     * @return an immutable map where the key is the project name (as a string)
     *         and the value is a list containing tasks due today for that project.
     */
    public Map<String, List<Task>> getTodaysTasks() {
        Map<String, List<Task>> todaysTasks = new LinkedHashMap<>();
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            for (Task task : project.getValue()) {
                Date deadline = task.getDeadline();
                if (deadline != null && deadline.toString().equals(today)) {
                    todaysTasks.put(project.getKey(), Collections.singletonList(task));
                }
            }
        }
        return Collections.unmodifiableMap(todaysTasks);
    }

    /**
     * Retrieves all tasks grouped by their deadlines and projects, in that order.
     *
     * @return an immutable map where the key is the deadline date,
     *         the value is an immutable map of project names to an
     *         immutable list of tasks associated with that deadline.
     */
    public Map<Date, Map<String, List<Task>>> getTasksByDeadline() {
        Map<Date, Map<String, List<Task>>> projectsByDeadline = new LinkedHashMap<>();
        // Iterate over all the projects, their tasks and their respective deadlines
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            for (Task task : project.getValue()) {
                Date deadline = task.getDeadline();
                if (deadline != null) {
                    // If the deadline was not recorded before
                    if (!projectsByDeadline.containsKey(deadline)) {
                        projectsByDeadline.put(deadline, new LinkedHashMap<>());
                    }
                    // If the given deadline does not include the encountered project
                    if (!projectsByDeadline.get(deadline).containsKey(project.getKey())) {
                        projectsByDeadline.get(deadline).put(project.getKey(), new ArrayList<>());
                    }
                    // Add the task to the given project under the given deadline
                    projectsByDeadline.get(deadline).get(project.getKey()).add(task);
                }
            }
        }
        // Sort deadlines in ascending order
        List<Date> sortedDeadlines = new ArrayList<>(projectsByDeadline.keySet());
        sortedDeadlines.sort(Comparator.naturalOrder());
        Map<Date, Map<String, List<Task>>> sortedTasks = new LinkedHashMap<>();

        for (Date deadline : sortedDeadlines) {
            Map<String, List<Task>> projectsCopy = new LinkedHashMap<>();
            for (Map.Entry<String, List<Task>> e : projectsByDeadline.get(deadline).entrySet()) {
                projectsCopy.put(e.getKey(), List.copyOf(e.getValue()));
            }
            sortedTasks.put(deadline, Collections.unmodifiableMap(projectsCopy));
        }
        return Collections.unmodifiableMap(sortedTasks);
    }

    /**
     * Retrieves a map of tasks that do not have a deadline, grouped by their associated project names.
     *
     * @return an immutable map where the key is the project name (as a string)
     *         and the value is a list of tasks without deadlines in that project.
     */
    public Map<String, List<Task>> getTasksWithoutDeadline() {
        Map<String, List<Task>> noDeadlineTasks = new TreeMap<>();
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            List<Task> projectTasks = new ArrayList<>();
            for (Task task : project.getValue()) {
                if (task.getDeadline() == null) {
                    projectTasks.add(task);
                }
            }
            if (!projectTasks.isEmpty()) {
                noDeadlineTasks.put(project.getKey(), projectTasks);
            }
        }
        return Collections.unmodifiableMap(noDeadlineTasks);
    }

    /**
     * Adds a new project to the task list. If the project name already exists,
     * no new project is added.
     *
     * @param name the name of the project to be added
     */
    public void addProject(String name) {
        projects.put(name.trim(), new ArrayList<>());
    }

    /**
     * Adds a new task to the specified project with the given description.
     * A unique ID is assigned to the task, and the task is initially marked as not done.
     *
     * @param project the name of the project to which the task should be added
     * @param description the description of the task to be added
     * @return true if the task was successfully added, false otherwise
     */
    public boolean addTask(String project, String description) {
        List<Task> projectTasks = projects.get(project);
        return projectTasks.add(new Task(nextId(), description, false));
    }

    /**
     * Updates the "done" status of a task with the specified ID.
     * Iterates through all projects and tasks to locate the task with the given ID,
     * then updates its status to the specified value.
     *
     * @param id the unique identifier of the task whose status is to be updated
     * @param done the new "done" status to be set for the task
     * @return true if the task is found and its status is successfully updated,
     *         false if no task with the specified ID exists
     */
    public boolean setDone(int id, boolean done) {
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDone(done);
                    return true;
                }
            }
        }
        return false;
    }

    private long nextId() {
        return ++lastId;
    }
}
