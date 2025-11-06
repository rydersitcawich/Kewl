package main.java.org.sprinting.model;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Represents a multiprocessor / agent in the sprinting game.
 */
public class TaskRunner {
    private final String id;
    private final Queue<Task> taskQueue;
    private RunnerState state;
    private boolean isSprinting;
    private final double sprintThreshold; // placeholder threshold for utility-based sprinting
    private final double probCooling; //placeholder probability to stay in cooling state
    private final double probRecovery; //placeholder probability to stay in recovery state

    public TaskRunner(String id, double sprintThreshold) {
        this.id = id;
        this.taskQueue = new LinkedList<>();
        this.state = RunnerState.ACTIVE;
        this.isSprinting = false;
        this.sprintThreshold = sprintThreshold;
        this.probCooling = 0.6; // placeholder
        this.probRecovery = 0.7; // placeholder
    }

    public void addTask(Task task) {
        taskQueue.add(task);
    }

    public int getTotalWork() {
        return taskQueue.stream().mapToInt(Task::getDuration).sum();
    }

    /**
     * Determines whether to sprint based on calculated utility and threshold.
     */
    public void evaluateSprint() {
        if (state != RunnerState.ACTIVE) {
            return;
        }

        double utility = calculateUtility(); // placeholder
        if (utility > sprintThreshold) {
            startSprint();
        }
    }

    private void startSprint() {
        isSprinting = true;
    }

    private double calculateUtility() {
        // TODO: replace with actual utility computation
        return Math.random(); // placeholder
    }

    public void executeEpoch() {
        if (!taskQueue.isEmpty()) {
            Task current = taskQueue.peek();
            current.executeEpoch(this.isSprinting());
            if (current.getState() == TaskState.COMPLETED) {
                taskQueue.poll();
            }
        }
    }

    public void updateState() {
        // how do we take into account going from active to recovery state
        if (isSprinting) {
            state = RunnerState.COOLING;
            isSprinting = false;
        } else if (state == RunnerState.COOLING && Math.random() > probCooling) {
            state = RunnerState.ACTIVE;
        } else if (state == RunnerState.RECOVERY && Math.random() > probRecovery) {
            state = RunnerState.ACTIVE;
        }
    }

    public RunnerState getState() {
        return state;
    }

    public boolean canSprint() {
        return state == RunnerState.ACTIVE;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("TaskRunner{id='%s', state=%s, sprinting=%s, totalWork=%d}",
                id, state, isSprinting, getTotalWork());
    }
}
