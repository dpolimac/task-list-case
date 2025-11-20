package com.ortecfinance.tasklist;

import java.text.SimpleDateFormat;
import java.util.*;

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
     * Collects tasks grouped by their deadlines and projects, then prints the organized tasks.
     *
     * This method categorizes all tasks into two groups:
     * 1. Tasks with specific deadlines, organized by them and their corresponding projects.
     * 2. Tasks with no specified deadlines.
     *
     * First {@code getTasksByDeadline} is used to structure tasks based on their deadlines and projects.
     * Tasks without a deadline are added to a separate list.
     *
     * Tasks with deadlines are printed using {@code printDeadlines},
     * while tasks without deadlines are printed using {@code printNoDeadlines}.
     */
    private void viewByDeadline() {
        Map<Date, Map<String, List<Task>>> tasksByDeadline = new LinkedHashMap<>();
        Map<String, List<Task>> noDeadlineTasks = new LinkedHashMap<>();

        groupTasks(tasksByDeadline, noDeadlineTasks);
        printDeadlines(tasksByDeadline, noDeadlineTasks);
    }

    /**
     * Prints a list of tasks grouped by their project names.
     * The projects are sorted alphabetically, and tasks are displayed for each project.
     *
     * @param projects a map where the key is the project name (as a String)
     *                 and the value is a list of tasks (List<Task>) associated with the project.
     */
    private void printProject(Map<String, List<Task>> projects) {
        if (!projects.isEmpty()) {
            List<String> projectNames = new ArrayList<>(projects.keySet());
            Collections.sort(projectNames);
            for (String projectName : projectNames) {
                out.printf("     %s:%n", projectName);
                for (Task task : projects.get(projectName)) {
                    out.printf("       \t%d: %s%n", task.getId(), task.getDescription());
                }
            }
        }
    }

    private void show() {
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            out.println(project.getKey());
            for (Task task : project.getValue()) {
                out.printf("    [%c] %d: %s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription());
            }
            out.println();
        }
    }

    private void add(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        String subcommand = subcommandRest[0];
        if (subcommand.equals("project")) {
            addProject(subcommandRest[1]);
        } else if (subcommand.equals("task")) {
            String[] projectTask = subcommandRest[1].split(" ", 2);
            addTask(projectTask[0], projectTask[1]);
        }
    }

    private void addProject(String name) {
        tasks.put(name.trim(), new ArrayList<>());
    }

    private void addTask(String project, String description) {
        List<Task> projectTasks = tasks.get(project);
        if (projectTasks == null) {
            out.printf("Could not find a project with the name \"%s\".", project);
            out.println();
            return;
        }
        projectTasks.add(new Task(nextId(), description, false));
    }

    private void check(String idString) {
        setDone(idString, true);
    }

    private void uncheck(String idString) {
        setDone(idString, false);
    }

    private void setDone(String idString, boolean done) {
        int id = Integer.parseInt(idString);
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDone(done);
                    return;
                }
            }
        }
        out.printf("Could not find a task with an ID of %d.", id);
        out.println();
    }

    private void help() {
        out.println("Commands:");
        out.println("  show");
        out.println("  today");
        out.println("  view-by-deadline");
        out.println("  add project <project name>");
        out.println("  add task <project name> <task description>");
        out.println("  check <task ID>");
        out.println("  uncheck <task ID>");
        out.println("  deadline <task ID> <date>");
        out.println();
    }

    private void error(String command) {
        out.printf("I don't know what the command \"%s\" is.", command);
        out.println();
    }

    private long nextId() {
        return ++lastId;
    }
}
