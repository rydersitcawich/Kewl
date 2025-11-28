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
        //generate utility from normal distribution [0,1]
        this.utility = clampedNormal(0.5, 0.15);
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
