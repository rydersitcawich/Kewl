package main.java.org.sprinting;

import java.util.*;

/**
 * Represents one multiprocessor.
 * Each runner maintains its own local queue of tasks.
 */
public class TaskRunner {
    private final String id;
    private final Queue<Task> localQueue;
    private int totalWork; // total remaining EpochUnits

    public TaskRunner(String id) {
        this.id = id;
        this.localQueue = new LinkedList<>();
        this.totalWork = 0;
    }

    public String getId() {
        return id;
    }

    public int getTotalWork() {
        return totalWork;
    }

    public void addTask(Task task) {
        localQueue.add(task);
        totalWork += task.getRemainingEpochUnits();
    }

    public void runAllTasks() {
        System.out.println("Running tasks on " + id + "...");
        while (!localQueue.isEmpty()) {
            Task current = localQueue.poll();
            while (current.getState() != TaskState.COMPLETED) {
                current.executeEpoch();
            }
            totalWork -= current.getRemainingEpochUnits();
        }
    }

    @Override
    public String toString() {
        return id + " [Total Work=" + totalWork + ", Tasks in Queue=" + localQueue.size() + "]";
    }
}
