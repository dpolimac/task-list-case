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
