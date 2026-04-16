package org.sprinting.model;

import java.util.Random;

public class Task {
    private final int id;
    private final int duration; // EpochUnits
    private double remainingEpochUnits;
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
        // Bimodal matching Spark workloads: ~50% low-parallelism, ~50% high-parallelism
        Random rand = new Random();
        if (rand.nextDouble() < 0.5) {
            this.utility = clampedNormal(0.15, 0.10); // low-benefit phases
        } else {
            this.utility = clampedNormal(0.75, 0.15); // high-benefit phases
        }
        numTasksCreated++;
        System.out.println("Generated utility " + this.utility + " for task " + id);
    }

    public double getDuration() {
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

    // Paper reports 2-7x realized speedup (12c@2.7GHz vs 3c@1.2GHz = 9x theoretical).
    // Using conservative midpoint of reported range.
    public static final double MAX_SPEEDUP = 4.0;

    public void executeEpoch(boolean isSprinting) {
        if (state == TaskState.COMPLETED) return;

        state = TaskState.RUNNING;
        if (isSprinting) {
            double sprintMultiplier = 1.0 + utility * (MAX_SPEEDUP - 1.0);
            remainingEpochUnits -= sprintMultiplier;
        } else {
            remainingEpochUnits -= 1.0;
        }
        if (remainingEpochUnits <= 0) {
            remainingEpochUnits = 0.0;
            state = TaskState.COMPLETED;
        }
    }

    public static double clampedNormal(double mean, double std) {
        double x = mean + std * new Random().nextGaussian();
        return Math.max(0.0, Math.min(1.0, x));
    }

    @Override
    public String toString() {
        return String.format("Task{id='%d', remaining=%.1f, state=%s}", id, remainingEpochUnits, state);
    }
}
