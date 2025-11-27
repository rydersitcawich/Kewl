package org.sprinting.model;

public class Task {
    private final int id;
    private final int duration; // EpochUnits
    private int remainingEpochUnits;
    private TaskState state;

    public Task(int id, int duration) {
        this.id = id;
        this.duration = duration;
        this.remainingEpochUnits = duration;
        this.state = TaskState.PENDING;
    }

    public int getDuration() {
        return remainingEpochUnits;
    }

    public TaskState getState() {
        return state;
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

    @Override
    public String toString() {
        return String.format("Task{id='%d', remaining=%d, state=%s}", id, remainingEpochUnits, state);
    }
}
