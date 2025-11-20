package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class TaskList implements Runnable {
    private static final String QUIT = "quit";

    private final Map<String, List<Task>> tasks = new LinkedHashMap<>();
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    private final BufferedReader in;
    private final PrintWriter out;

    private long lastId = 0;

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new TaskList(in, out).run();
    }

    public TaskList(BufferedReader reader, PrintWriter writer) {
        this.in = reader;
        this.out = writer;
    }

    public void run() {
        out.println("Welcome to TaskList! Type 'help' for available commands.");
        while (true) {
            out.print("> ");
            out.flush();
            String command;
            try {
                command = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (command.equals(QUIT)) {
                break;
            }
            execute(command);
        }
    }

    private void execute(String commandLine) {
        String[] commandRest = commandLine.split(" ", 2);
        String command = commandRest[0];
        switch (command) {
            case "deadline":    // Added
                addDeadline(commandRest[1]);
                break;
            case "view-by-deadline":    // Added
                viewByDeadline();
                break;
            case "today":       // Added
                today();
                break;
            case "show":
                show();
                break;
            case "add":
                add(commandRest[1]);
                break;
            case "check":
                check(commandRest[1]);
                break;
            case "uncheck":
                uncheck(commandRest[1]);
                break;
            case "help":
                help();
                break;
            default:
                error(command);
                break;
        }
    }

    /**
     * Adds or updates the deadline for a given task based on its ID.
     * The command should include the task ID and the deadline in the format "dd-MM-yyyy".
     * If the task ID is found, the corresponding task's deadline will be updated;
     * otherwise the method outputs "No task with the given ID was found."
     *
     * @param commandLine the input string containing the task ID and deadline,
     *                    separated by a space, e.g. "1 25-12-2023".
     *                    The first part represents the task ID (an integer),
     *                    and the second part represents the deadline (dd-MM-yyyy).
     * @throws RuntimeException if the date format specified in the commandLine is invalid.
     */
    void addDeadline(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        int id = Integer.parseInt(subcommandRest[0]);
        Date deadline;

        try {
            deadline = formatter.parse(subcommandRest[1]);
        } catch (ParseException e) {
            out.println("Invalid date format.");
            throw new RuntimeException(e);  // Not sure if throwing an exception is the best way to handle this (?)
        }

        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDeadline(deadline);
                    return;
                }
            }
        }
        out.println("No task with the given ID was found.");
    }

    /**
     * Displays all tasks from each project that have a deadline matching today's date.
     * If there are no projects with tasks due today's deadline, the method prints nothing.
     *
     * Each project name with matching tasks is printed, followed by the details of the tasks:
     * - Task ID
     * - Task description
     * - Completion status (checked or unchecked)
     */
    void today() {
        String today = formatter.format(new Date());

        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            String projectName = project.getKey();
            List<Task> projectTasks = project.getValue();
            List<Task> todaysTasks = new ArrayList<>();

            for (Task task : projectTasks) {
                Date deadline = task.getDeadline();
                if (deadline != null && formatter.format(deadline).equals(today)) {
                    todaysTasks.add(task);
                }
            }

            if (!todaysTasks.isEmpty()) {
                out.println(projectName);
                for (Task task : todaysTasks) {
                    out.printf("    [%c] %d: %s%n",
                            (task.isDone() ? 'x' : ' '),
                            task.getId(),
                            task.getDescription());
                }
                out.println();
            }
        }
    }

    /**
     * Organizes tasks into a structure based on their deadlines, and their projects.
     *
     * @param tasksByDeadline a map where tasks are grouped by their deadlines and projects, respectively.
     * @param noDeadlineTasks a list that collects tasks that do not have a deadline specified.
     */
    private void groupTasks(Map<Date, Map<String, List<Task>>> tasksByDeadline,
                                    Map<String, List<Task>> noDeadlineTasks) {
        // Iterate over all the projects, their tasks and their respective deadlines
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                Date deadline = task.getDeadline();
                if (deadline == null) {
                    if (!noDeadlineTasks.containsKey(project.getKey())) {
                        noDeadlineTasks.put(project.getKey(), new ArrayList<>());
                    }
                    noDeadlineTasks.get(project.getKey()).add(task);
                } else {
                    // If the deadline was not recorded before
                    if (!tasksByDeadline.containsKey(deadline)) {
                        tasksByDeadline.put(deadline, new LinkedHashMap<>());
                    }
                    // If the given deadline does not include the encountered project
                    if (!tasksByDeadline.get(deadline).containsKey(project.getKey())) {
                        tasksByDeadline.get(deadline).put(project.getKey(), new ArrayList<>());
                    }
                    // Add the task to the given project under the given deadline
                    tasksByDeadline.get(deadline).get(project.getKey()).add(task);
                }
            }
        }
    }

    /**
     * Prints a list of tasks grouped by their deadlines and project names, respectively.
     * Each deadline is displayed in ascending order along with the projects and associated tasks for that date.
     *
     * @param tasksByDeadline a map where keys are deadlines (Date objects) and values are maps of projects.
     *                        Each project map has project names as keys and corresponding task lists as values.
     */
    private void printDeadlines(Map<Date, Map<String, List<Task>>> tasksByDeadline,
                                Map<String, List<Task>> noDeadlineTasks) {
        // Sort by deadlines
        List<Date> sortedDeadlines = new ArrayList<>(tasksByDeadline.keySet());
        sortedDeadlines.sort(Comparator.naturalOrder());

        // Printing tasks with a deadline
        for (Date deadline : sortedDeadlines) {
            String formattedDeadline = formatter.format(deadline);
            out.println(formattedDeadline + ":");
            Map<String, List<Task>> projectsForDate = tasksByDeadline.get(deadline);
            printProject(projectsForDate);
        }

        // Print tasks without a deadline
        if (!noDeadlineTasks.isEmpty()) {
            out.println("No deadline:");
            printProject(noDeadlineTasks);
        }
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
