package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.PrintWriter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Console implements Runnable {

    private static final String QUIT = "quit";

    private final BufferedReader in;
    private final PrintWriter out;
    private final Handler handler;

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new Console(in, out).run();
    }

    public Console(BufferedReader reader, PrintWriter writer) {
        this.in = reader;
        this.out = writer;
        this.handler = new Handler(new TaskList(), out);
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
            handler.execute(command);
        }
    }
}
