package org.sprinting.model;

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
    private boolean sprintedThisEpoch; // for GUI display: true during the epoch a sprint occurred
    private double sprintThreshold; // placeholder threshold for utility-based sprinting
    private double hydrogelState = 1.0; // 1.0 = fully saturated, 0.0 = depleted
    private int epochsInRecovery; //number of epochs till we have fully recovered. 0 means we are in active state. When we have a power failure, we set this to some positive integer.
    private final int POWER_EPOCHS = 8; // rack power recovery ~8 epochs (paper: pr=0.88)
    private double cachedTotalWork = 0.0; // cached sum of remaining epoch units across all tasks

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
        cachedTotalWork += task.getDuration();
    }

    public double getTotalWork() {
        return cachedTotalWork;
    }

    /**
     * Determines whether to sprint based on calculated utility and threshold.
     */
    public void evaluateSprint() {
        isSprinting = false;
        sprintedThisEpoch = false;
        if (!canSprint()) return;
        double utility = calculateUtility();
        if (utility > sprintThreshold) {
            startSprint();
        }
    }

    private void startSprint() {
        isSprinting = true;
        sprintedThisEpoch = true;
    }

    private double calculateUtility() {
        Task currTask = taskQueue.peek();
        if (currTask == null) {
            return 0.0;
        }
        return currTask.getUtility();
    }

    public void executeEpoch() {
        if (!taskQueue.isEmpty()) {
            Task current = taskQueue.peek();
            double before = current.getDuration();
            current.executeEpoch(this.isSprinting());
            cachedTotalWork -= (before - current.getDuration());
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
        this.sprintedThisEpoch = false;
    }

    public boolean isInPowerRecovery() {
        return epochsInRecovery > 0;
    }

    public boolean canSprint() {
        return epochsInRecovery == 0 && hydrogelState >= 1.0;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public boolean sprintedThisEpoch() {
        return sprintedThisEpoch;
    }

    public boolean isWorking() {
        return !taskQueue.isEmpty();
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

    public double getCurrentUtility() {
        Task t = taskQueue.peek();
        return t == null ? 0.0 : t.getUtility();
    }

    public void setSprintThreshold(double newThreshold) {
        this.sprintThreshold = newThreshold;
    }

    public double getHydrogelState() {
        return hydrogelState;
    }

    public void setHydrogelState(double state) {
        this.hydrogelState = state;
    }

    @Override
    public String toString() {
        return String.format("TaskRunner{id='%d', sprinting=%s, totalWork=%.1f}",
                ID, isSprinting, getTotalWork());
    }
}
