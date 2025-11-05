package main.java.org.sprinting;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskRunner sequentially executes a list of tasks.
 * Later, this can be expanded to simulate multiple multiprocessors or a greedy scheduler.
 */
public class TaskRunner {
    private final List<Task> tasks;

    public TaskRunner() {
        this.tasks = new ArrayList<>();
    }

    public void addTask(Task task) {
        tasks.add(task);
    }

    public void runAllTasks() {
        System.out.println("Starting task execution...");

        for (Task task : tasks) {
            while (task.getState() != TaskState.COMPLETED) {
                task.executeEpoch();
            }
        }

        System.out.println("All tasks completed.");
    }

    public static void main(String[] args) {
        TaskRunner runner = new TaskRunner();
        runner.addTask(new Task("A", 3));
        runner.addTask(new Task("B", 2));
        runner.addTask(new Task("C", 5));

        runner.runAllTasks();
    }
}
