package main.java.org.sprinting.model;

import java.util.LinkedList;
import java.util.Queue;


/**
 * Represents a multiprocessor / agent in the sprinting game.
 */

public class TaskRunner { 
    private final int ID;
    private final int SERVER_ID;
    private final int RACK_ID;
    private final Queue<Task> taskQueue;
    private boolean isSprinting;
    private final double sprintThreshold; // placeholder threshold for utility-based sprinting
    private int epochsInRecovery; //number of epochs till we have fully recovered. 0 means we are in active state. When we have a power or thermal failure, we set this to some positive integer.
    private final int COOLING_EPOCHS = 5; //placeholder for num epochs to recover from thermal failure
    private final int POWER_EPOCHS = 5; //placeholder for num epochs to recover from power failure

    public TaskRunner(int id, double sprintThreshold, int serverId, int rackId) {
        this.ID = id;
        this.SERVER_ID = serverId;
        this.RACK_ID = rackId;
        this.taskQueue = new LinkedList<>();
        this.isSprinting = false;
        this.sprintThreshold = sprintThreshold;
        this.epochsInRecovery = 0;
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
        if (epochsInRecovery > 0) { // we are recovering
            return;
        }

        double utility = calculateUtility(); 
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
        if (epochsInRecovery > 0) {
            epochsInRecovery--;
        }
    }

    public void updateEpochsInRecoveryForPowerFailure() {
        this.epochsInRecovery = Math.max(epochsInRecovery, POWER_EPOCHS);
        this.isSprinting = false;
    }

    public void updateEpochsInRecoveryForThermalFailure() {
        this.epochsInRecovery = Math.max(epochsInRecovery, COOLING_EPOCHS);
        this.isSprinting = false;
    }

    public boolean canSprint() {
        return epochsInRecovery == 0;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public int getId() {
        return ID;
    }

    public int getServerId() {
        return SERVER_ID;
    }

    public int getRackId() {
        return RACK_ID;
    }

    @Override
    public String toString() {
        return String.format("TaskRunner{id='%d', sprinting=%s, totalWork=%d}",
                ID, isSprinting, getTotalWork());
    }
}
