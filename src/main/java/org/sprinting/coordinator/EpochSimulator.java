package main.java.org.sprinting.coordinator;

import main.java.org.sprinting.model.TaskRunner;

import java.util.List;

public class EpochSimulator {
    private final List<TaskRunner> runners;

    public EpochSimulator(List<TaskRunner> runners) {
        this.runners = runners;
    }

    /**
     * Simulate one epoch:
     * - Each runner decides whether to sprint based on utility & threshold.
     * - Each runner progresses one epoch of tasks.
     * - States update at the end of the epoch.
     */
    public void runEpoch() {
        System.out.println("\n--- New Epoch ---");

        for (TaskRunner runner : runners) {
            runner.evaluateSprint();  // decide whether to sprint
            runner.executeEpoch();    // make progress on tasks
            runner.updateState();     // update cooling/recovery state

            System.out.println(runner);
        }
    }
}

