package org.sprinting.model;

import java.util.Random;

public class Task {
    private final int id;
    private final int duration; // EpochUnits
    private int remainingEpochUnits;
    private TaskState state;
    private double utility;
    private static int numTasksCreated = 0;

    public Task(int id, int duration, double utility) {
        this.id = id;
        this.duration = duration;
        this.remainingEpochUnits = duration;
        this.state = TaskState.PENDING;
        this.utility = utility;
        numTasksCreated++;
    }

    public Task(int id, int duration) {
        this.id = id;
        this.duration = duration;
        this.remainingEpochUnits = duration;
        this.state = TaskState.PENDING;
        // 70% of tasks have low utility, 30% have high utility
        Random rand = new Random();
        if (rand.nextDouble() < 0.7) {
            this.utility = clampedNormal(0.2, 0.08); // low-benefit tasks
        } else {
            this.utility = clampedNormal(0.8, 0.08); // high-benefit tasks
        }
        numTasksCreated++;
        System.out.println("Generated utility " + this.utility + " for task " + id);
    }

    public int getDuration() {
        return remainingEpochUnits;
    }

    public TaskState getState() {
        return state;
    }

    public static int getNumberOfTasksCreated() {
        return numTasksCreated;
    }

    public double getUtility() {
        return utility;
    }

    public void executeEpoch(boolean isSprinting) {
        if (state == TaskState.COMPLETED) return;

        state = TaskState.RUNNING;
        if (isSprinting) {
            remainingEpochUnits -= 2; // sprinting processes 2 units per epoch (assumption)
        } else {
            remainingEpochUnits--;
        }
        if (remainingEpochUnits <= 0) {
            remainingEpochUnits = 0;
            state = TaskState.COMPLETED;
        }
    }

    public static double clampedNormal(double mean, double std) {
        double x = mean + std * new Random().nextGaussian();
        return Math.max(0.0, Math.min(1.0, x));
    }

    @Override
    public String toString() {
        return String.format("Task{id='%d', remaining=%d, state=%s}", id, remainingEpochUnits, state);
    }
}
