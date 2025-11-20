package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Console implements Runnable {

    private static final String QUIT = "quit";
    private final TaskList taskList;
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    private final BufferedReader in;
    private final PrintWriter out;

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new Console(in, out).run();
    }

    public Console(BufferedReader reader, PrintWriter writer) {
        this.in = reader;
        this.out = writer;
        this.taskList = new TaskList();
    }

    @Override
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
            case "show", "today" -> displayHandler(commandRest[0]);
            case "deadline" -> handleDeadline(commandRest[1]);
            case "view-by-deadline" -> viewByDeadline();
            case "add" -> handleAdd(commandRest[1]);
            case "check" -> handleCheck(commandRest[1], true);
            case "uncheck" -> handleCheck(commandRest[1], false);
            case "help" -> help();
            default -> error(command);
        }
    }

    /**
     * Handles the addition of a project or task based on the given input arguments.
     *
     * @param args the input string containing the command and its arguments.
     *             Expected format: "add project <project name>" to add a project,
     *             or "add task <project name> <task description>" to add a task to a project.
     */
    private void handleAdd(String args) {
        String[] subcommand = args.split(" ", 2);
        if ("project".equals(subcommand[0])) {
            taskList.addProject(subcommand[1]);
        } else if ("task".equals(subcommand[0])) {
            String[] projectTask = subcommand[1].split(" ", 2);
            if (projectTask.length < 2) {
                out.println("Invalid command format. Expected format: add task <project name> <task description>");
                return;
            }
            boolean isAdded = taskList.addTask(projectTask[0], projectTask[1]);
            if (!isAdded) {
                out.println("No project with the given name was found.");
            }
        }
    }

    /**
     * Marks a task as completed or not completed based on the provided a valid task ID and status.
     *
     * @param args the input string containing the task ID (integer).
     * @param checked the status to set for the specified task (boolean).
     * @throws RuntimeException if the task ID is not a valid number.
     */
    private void handleCheck(String args, boolean checked) {
        if (args.isEmpty()) {
            out.println("Invalid command format. Expected format: check <task ID>");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args);
            boolean isChecked = taskList.setDone(id, checked);
            if (!isChecked) {
                out.println("No task with the given ID was found.");
            }
        } catch (NumberFormatException e) {
            out.println("Invalid task ID.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the addition or update of a deadline (Date) for a specific task identified by its ID (integer).
     *
     * @param args a string containing the task ID and the deadline, separated by a space.
     *             The deadline must follow the format "dd-MM-yyyy".
     * @throws RuntimeException if the task ID is not a valid number.
     * @throws RuntimeException if the deadline format is invalid.
     */
    private void handleDeadline(String args) {
        String[] subcommand = args.split(" ", 2);
        int id;
        try {
            id = Integer.parseInt(subcommand[0]);
        } catch (NumberFormatException e) {
            out.println("Invalid task ID.");
            throw new RuntimeException(e);
        }
        Date deadline;
        try {
            deadline = formatter.parse(subcommand[1]);
        } catch (java.text.ParseException e) {
            out.println("Invalid deadline format. Expected format: dd-MM-yyyy.");
            throw new RuntimeException(e);
        }

        boolean isAdded = taskList.addDeadline(id, deadline);
        if (!isAdded) {
            out.println("No task with the given ID was found.");
        }
    }

    /**
     * Displays either all tasks or tasks with their deadline today, depending on the argument passed.
     *
     * @param args the input command specifying the type of tasks to display.
     *             Acceptable values are:
     *             - "today": Displays tasks due today.
     *             - "show": Displays all projects and their associated tasks.
     *             If the argument is invalid, an error message is printed.
     */
    private void displayHandler(String args) {
        Map<String, List<Task>> projects;

        if ("today".equals(args)) {
            projects = taskList.getTodaysTasks();
        } else if ("show".equals(args)) {
            projects = taskList.getAllProjects();
        } else {
            error(args);
            return;
        }
        for (Map.Entry<String, List<Task>> project : projects.entrySet()) {
            out.println(project.getKey());
            for (Task task : project.getValue()) {
                out.printf("    [%c] %d: %s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription());
            }
            out.println();
        }
    }

    /**
     * Displays the list of available commands for the console application.
     */
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
}
