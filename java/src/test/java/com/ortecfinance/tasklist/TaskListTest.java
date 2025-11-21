package com.ortecfinance.tasklist;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TaskListTest {

    private static Date dateOf(int year, int month, int day) {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Test
    @DisplayName("addProject creates an empty project; addTask adds tasks; getAllProjects is unmodifiable")
    void addProject_addTask_getAllProjects() {
        TaskList list = new TaskList();

        list.addProject("p1");
        assertTrue(list.addTask("p1", "t1"));
        assertTrue(list.addTask("p1", "t2"));

        Map<String, List<Task>> all = list.getAllProjects();
        assertEquals(1, all.size());
        assertTrue(all.containsKey("p1"));
        assertEquals(2, all.get("p1").size());

        assertThrows(UnsupportedOperationException.class, () -> all.put("x", List.of()));
    }

    @Test
    @DisplayName("addTask returns false when project does not exist")
    void addTask_unknownProject() {
        TaskList list = new TaskList();
        assertFalse(list.addTask("nope", "t1"));
    }

    @Test
    @DisplayName("setDone toggles task completion by id; returns false for unknown id")
    void setDone_updates_status() {
        TaskList list = new TaskList();
        list.addProject("p");
        list.addTask("p", "t1"); // id = 1
        list.addTask("p", "t2"); // id = 2

        assertTrue(list.setDone(1, true));
        Map<String, List<Task>> all = list.getAllProjects();
        Task t1 = all.get("p").get(0);
        Task t2 = all.get("p").get(1);
        assertTrue(t1.isDone());
        assertFalse(t2.isDone());

        assertTrue(list.setDone(1, false));
        all = list.getAllProjects();
        assertFalse(all.get("p").get(0).isDone());

        assertFalse(list.setDone(999, true));
    }

    @Test
    @DisplayName("addDeadline sets deadline by task id; returns false for unknown id")
    void addDeadline_sets_deadline() {
        TaskList list = new TaskList();
        list.addProject("p");
        list.addTask("p", "t1"); // id = 1

        Date d = dateOf(2024, 1, 2);
        assertTrue(list.addDeadline(1, d));
        Map<String, List<Task>> all = list.getAllProjects();
        assertEquals(d, all.get("p").get(0).getDeadline());

        assertFalse(list.addDeadline(2, d));
    }

    @Test
    @DisplayName("getTasksWithoutDeadline returns only tasks with null deadline; result is unmodifiable")
    void getTasksWithoutDeadline_only_null_deadlines() {
        TaskList list = new TaskList();
        list.addProject("p");
        list.addTask("p", "t1"); // id = 1
        list.addTask("p", "t2"); // id = 2
        list.addDeadline(2, dateOf(2024, 1, 1));

        Map<String, List<Task>> res = list.getTasksWithoutDeadline();
        assertTrue(res.containsKey("p"));
        List<Task> tasks = res.get("p");
        assertEquals(1, tasks.size());
        assertEquals(1, tasks.get(0).getId());

        assertThrows(UnsupportedOperationException.class, () -> res.put("x", List.of()));
    }

    @Test
    @DisplayName("getTasksByDeadline groups by exact deadline and sorts dates ascending; results are unmodifiable")
    void getTasksByDeadline_groups_and_sorts() {
        TaskList list = new TaskList();
        list.addProject("A");
        list.addProject("B");
        list.addTask("A", "a1"); // id 1
        list.addTask("A", "a2"); // id 2
        list.addTask("B", "b1"); // id 3

        Date d1 = dateOf(2024, 1, 1);
        Date d2 = dateOf(2024, 2, 1);
        list.addDeadline(1, d2);
        list.addDeadline(2, d1);
        list.addDeadline(3, d1);

        Map<Date, Map<String, List<Task>>> byDeadline = list.getTasksByDeadline();
        List<Date> keys = new ArrayList<>(byDeadline.keySet());
        assertEquals(List.of(d1, d2), keys, "deadlines should be sorted ascending");

        Map<String, List<Task>> onD1 = byDeadline.get(d1);
        assertTrue(onD1.containsKey("A"));
        assertTrue(onD1.containsKey("B"));
        assertEquals(1, onD1.get("A").size());
        assertEquals(1, onD1.get("B").size());

        Map<String, List<Task>> onD2 = byDeadline.get(d2);
        assertTrue(onD2.containsKey("A"));
        assertEquals(1, onD2.get("A").size());

        assertThrows(UnsupportedOperationException.class, () -> byDeadline.put(new Date(), Map.of()));
        assertThrows(UnsupportedOperationException.class, () -> byDeadline.get(d1).put("X", List.of()));
        assertThrows(UnsupportedOperationException.class, () -> byDeadline.get(d1).get("A").add(new Task(999, "x", false)));
    }

    @Test
    @DisplayName("getTodaysTasks returns only tasks due today grouped by project")
    void getTodaysTasks_filters_by_today() {
        TaskList list = new TaskList();
        list.addProject("P");
        list.addTask("P", "t1"); // id 1
        list.addTask("P", "t2"); // id 2

        // t1 due today, t2 due tomorrow
        Date today = dateOf(LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth());
        Date tomorrow = dateOf(LocalDate.now().plusDays(1).getYear(),
                LocalDate.now().plusDays(1).getMonthValue(),
                LocalDate.now().plusDays(1).getDayOfMonth());

        list.addDeadline(1, today);
        list.addDeadline(2, tomorrow);

        Map<String, List<Task>> todays = list.getTodaysTasks();
        assertTrue(todays.containsKey("P"));
        List<Task> tasks = todays.get("P");
        assertEquals(1, tasks.size());
        assertEquals(1, tasks.get(0).getId());
    }
}