package main.java.org.sprinting;
import java.util.*;

/**
 * The Coordinator acts as a greedy scheduler.
 * It receives incoming tasks and assigns them to the multiprocessor (TaskRunner)
 * with the least total remaining work.
 */
public class Coordinator {
    private final List<TaskRunner> runners;
    private final Queue<Task> globalTaskQueue;

    public Coordinator(int numRunners) {
        this.runners = new ArrayList<>();
        this.globalTaskQueue = new LinkedList<>();

        for (int i = 0; i < numRunners; i++) {
            runners.add(new TaskRunner("MP-" + i));
        }
    }

    /**
     * Adds a task to the global task queue.
     */
    public void submitTask(Task task) {
        globalTaskQueue.add(task);
    }

    /**
     * Greedily assigns tasks to the least-loaded runner.
     */
    public void schedule() {
        while (!globalTaskQueue.isEmpty()) {
            Task nextTask = globalTaskQueue.poll();
        
            TaskRunner leastLoaded = Collections.min(runners, Comparator.comparingInt(TaskRunner::getTotalWork));

            leastLoaded.addTask(nextTask);
            System.out.println("Assigned Task " + nextTask.getId() + " to " + leastLoaded.getId());
        }
    }

    /**
     * Run all tasks across all runners.
     */
    public void executeAll() {
        System.out.println("Starting execution across all multiprocessors...");
        for (TaskRunner runner : runners) {
            runner.runAllTasks();
        }
        System.out.println("All multiprocessors completed their tasks.");
    }

    public void printStatus() {
        for (TaskRunner runner : runners) {
            System.out.println(runner);
        }
    }
}

