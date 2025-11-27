package org.sprinting.coordinator;

import org.sprinting.model.Task;
import org.sprinting.model.TaskRunner;

import java.util.Comparator;
import java.util.List;

/**
 * Greedy scheduler: assigns incoming tasks to the runner with least total work.
 */
public class GreedyScheduler {
    private final List<TaskRunner> runners;

    public GreedyScheduler(List<TaskRunner> runners) {
        this.runners = runners;
    }

    public void assignTask(Task task) {
        TaskRunner leastLoaded = runners.stream()
                .min(Comparator.comparingInt(TaskRunner::getTotalWork))
                .orElseThrow();

        leastLoaded.addTask(task);
        System.out.println("Assigned task " + task + " to " + leastLoaded.getId());
    }
}


