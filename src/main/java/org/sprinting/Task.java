package main.java.org.sprinting;

/**
 * Represents a unit of work in the system.
 * Each task requires a certain number of EpochUnits (EUs) to complete.
 */
public class Task {
    private final String id;
    private final int totalEpochUnits;
    private int remainingEpochUnits;
    private TaskState state;

    public Task(String id, int totalEpochUnits) {
        this.id = id;
        this.totalEpochUnits = totalEpochUnits;
        this.remainingEpochUnits = totalEpochUnits;
        this.state = TaskState.PENDING;
    }

    public String getId() {
        return id;
    }

    public int getRemainingEpochUnits() {
        return remainingEpochUnits;
    }

    public TaskState getState() {
        return state;
    }

    /**
     * Executes a single epoch worth of work.
     */
    public void executeEpoch() {
        if (state == TaskState.COMPLETED) {
            System.out.println("Task " + id + " is already completed.");
            return;
        }

        state = TaskState.RUNNING;
        if (remainingEpochUnits > 0) {
            remainingEpochUnits--;
        }

        if (remainingEpochUnits == 0) {
            state = TaskState.COMPLETED;
            System.out.println("Task " + id + " completed.");
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", remainingEpochUnits=" + remainingEpochUnits +
                ", state=" + state +
                '}';
    }
}
