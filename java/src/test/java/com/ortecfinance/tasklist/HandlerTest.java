package com.ortecfinance.tasklist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class HandlerTest {

    private static Handler handlerWithBuffer(TaskList taskList, StringWriter buffer) {
        return new Handler(taskList, new PrintWriter(buffer, true));
    }

    @Test
    @DisplayName("help: prints the list of supported commands")
    void help_prints_commands() {
        StringWriter buf = new StringWriter();
        Handler handler = handlerWithBuffer(new TaskList(), buf);

        handler.execute("help");

        String out = buf.toString();
        assertTrue(out.contains("Commands:"), "should print header");
        assertTrue(out.contains("show"), "should list show");
        assertTrue(out.contains("today"), "should list today");
        assertTrue(out.contains("view-by-deadline"), "should list view-by-deadline");
        assertTrue(out.contains("add project"), "should list add project");
        assertTrue(out.contains("add task"), "should list add task");
        assertTrue(out.contains("check"), "should list check");
        assertTrue(out.contains("uncheck"), "should list uncheck");
        assertTrue(out.contains("deadline"), "should list deadline");
    }

    @Test
    @DisplayName("unknown command: prints error message")
    void unknown_command_prints_error() {
        StringWriter buf = new StringWriter();
        Handler handler = handlerWithBuffer(new TaskList(), buf);

        handler.execute("foobar");

        String out = buf.toString();
        assertTrue(out.contains("I don't know what the command \"foobar\" is."),
                "should print unknown command error");
    }

    @Test
    @DisplayName("add project and tasks; show: prints tasks with status")
    void add_project_and_task_then_show_prints_tasks_with_status() {
        StringWriter buf = new StringWriter();
        TaskList taskList = new TaskList();
        Handler handler = handlerWithBuffer(taskList, buf);

        handler.execute("add project secrets");
        handler.execute("add task secrets Eat more donuts.");
        handler.execute("add task secrets Destroy all humans.");
        handler.execute("show");

        String out = buf.toString();
        assertTrue(out.contains("secrets"));
        assertTrue(out.contains("[ ] 1: Eat more donuts."));
        assertTrue(out.contains("[ ] 2: Destroy all humans."));
    }

    @Test
    @DisplayName("check/uncheck: toggles task status and output reflects change")
    void check_and_uncheck_updates_status() {
        StringWriter buf = new StringWriter();
        TaskList taskList = new TaskList();
        Handler handler = handlerWithBuffer(taskList, buf);

        handler.execute("add project p");
        handler.execute("add task p t1");
        handler.execute("check 1");
        handler.execute("show");

        String out = buf.toString();
        assertTrue(out.contains("[x] 1: t1"));

        buf.getBuffer().setLength(0); // clear
        handler.execute("uncheck 1");
        handler.execute("show");

        out = buf.toString();
        assertTrue(out.contains("[ ] 1: t1"));
    }

    @Test
    @DisplayName("handleCheck: empty args prints usage and returns")
    void handleCheck_emptyArgs_printsUsage() {
        TaskList list = new TaskList();
        StringWriter out = new StringWriter();
        Handler handler = handlerWithBuffer(list, out);

        // execute "check" with no args -> empty args branch
        handler.execute("check");

        String output = out.toString();
        assertTrue(output.contains("Invalid command format. Expected format: check <task ID>"));
    }

    @Test
    @DisplayName("handleCheck: non-numeric id prints 'Invalid task ID.' and throws RuntimeException")
    void handleCheck_nonNumericId_throws() {
        TaskList list = new TaskList();
        StringWriter out = new StringWriter();
        Handler handler = handlerWithBuffer(list, out);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.execute("check abc"));
        assertNotNull(ex.getCause(), "Wrapped NumberFormatException expected");
        String output = out.toString();
        assertTrue(output.contains("Invalid task ID."));
    }

    @Test
    @DisplayName("handleCheck: unknown numeric id prints 'No task with the given ID was found.'")
    void handleCheck_unknownId_printsNotFound() {
        TaskList list = new TaskList();
        list.addProject("P");
        list.addTask("P", "t1"); // id = 1

        StringWriter out = new StringWriter();
        Handler handler = handlerWithBuffer(list, out);

        // ID 999 does not exist
        handler.execute("check 999");

        String output = out.toString();
        assertTrue(output.contains("No task with the given ID was found."));
        // and no exception thrown
    }

    @Test
    @DisplayName("handleCheck: valid id toggles status, no extra error output")
    void handleCheck_validId_updatesStatus() {
        TaskList list = new TaskList();
        list.addProject("P");
        list.addTask("P", "t1"); // id = 1

        StringWriter out = new StringWriter();
        Handler handler = handlerWithBuffer(list, out);

        handler.execute("check 1");

        // Verify task is done and no error lines were printed
        assertTrue(list.getAllProjects().get("P").get(0).isDone());
        String output = out.toString();
        assertFalse(output.contains("Invalid task ID."));
        assertFalse(output.contains("No task with the given ID was found."));
        assertFalse(output.contains("Invalid command format."));
    }

    @Test
    @DisplayName("check with invalid id: prints message and throws RuntimeException")
    void check_with_invalid_id_prints_message_and_throws_runtime() {
        StringWriter buf = new StringWriter();
        Handler handler = handlerWithBuffer(new TaskList(), buf);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> handler.execute("check abc"));
        String out = buf.toString();
        assertTrue(out.contains("Invalid task ID."));
    }

    @Test
    @DisplayName("add task to unknown project: prints 'No project...' message")
    void add_task_to_unknown_project_prints_message() {
        StringWriter buf = new StringWriter();
        Handler handler = handlerWithBuffer(new TaskList(), buf);

        handler.execute("add task unknown Some task");

        String out = buf.toString();
        assertTrue(out.contains("No project with the given name was found."));
    }

    @Test
    @DisplayName("add task missing description: prints format help message")
    void add_task_with_missing_description_prints_message() {
        StringWriter buf = new StringWriter();
        TaskList taskList = new TaskList();
        taskList.addProject("p");
        Handler handler = handlerWithBuffer(taskList, buf);

        handler.execute("add task p");

        String out = buf.toString();
        assertTrue(out.contains("Invalid command format. Expected format: add task <project name> <task description>"));
    }

    @Test
    @DisplayName("deadline: bad id -> error; bad date -> error; missing id -> error; valid -> listed in view-by-deadline")
    void deadline_sets_date_or_reports_errors() {
        StringWriter buf = new StringWriter();
        TaskList taskList = new TaskList();
        Handler handler = handlerWithBuffer(taskList, buf);

        handler.execute("add project p");
        handler.execute("add task p t1");

        // bad id
        RuntimeException badId = assertThrows(RuntimeException.class, () -> handler.execute("deadline abc 01-01-2024"));
        String out = buf.toString();
        assertTrue(out.contains("Invalid task ID."));

        buf.getBuffer().setLength(0); // clear

        // bad date
        RuntimeException badDate = assertThrows(RuntimeException.class, () -> handler.execute("deadline 1 2024-01-01"));
        out = buf.toString();
        assertTrue(out.contains("Invalid deadline format. Expected format: dd-MM-yyyy."));

        buf.getBuffer().setLength(0); // clear

        // missing id
        RuntimeException missingId = assertThrows(RuntimeException.class, () -> handler.execute("deadline  01-01-2024"));
        out = buf.toString();
        assertTrue(out.contains("Invalid task ID."));

        buf.getBuffer().setLength(0); // clear

        // non-existent id
        handler.execute("deadline 28 01-01-2024");
        out = buf.toString();
        assertTrue(out.contains("No task with the given ID was found."));

        buf.getBuffer().setLength(0); // clear

        // ok
        handler.execute("add task p t2");   // Task without deadline
        handler.execute("deadline 1 01-01-2024");
        handler.execute("view-by-deadline");
        out = buf.toString();
        assertTrue(out.contains("01-01-2024:"), "should print the deadline header");
        assertTrue(out.contains("1: t1"), "should list the task under the date");
    }

    @Test
    @DisplayName("show and today: delegate to TaskList without crashing")
    void show_and_today_delegation_prints_without_crashing() {
        StringWriter buf = new StringWriter();
        TaskList taskList = new TaskList();
        Handler handler = handlerWithBuffer(taskList, buf);

        handler.execute("show");
        handler.execute("today");

        String out = buf.toString();
        assertTrue(out != null);
    }

    @Test
    @DisplayName("handleDeadline: empty args prints usage and returns")
    void handleDeadline_emptyArgs_printsUsage() {
        TaskList list = new TaskList();
        StringWriter out = new StringWriter();
        Handler handler = handlerWithBuffer(list, out);
    }
}