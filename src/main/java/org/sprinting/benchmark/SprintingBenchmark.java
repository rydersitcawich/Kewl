package org.sprinting.benchmark;

import org.sprinting.model.DataCenter;
import org.sprinting.model.Task;
import org.sprinting.model.TaskRunner;
import org.sprinting.model.TaskState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Compares the Bellman sprinting policy against a never-sprint baseline.
 * Both runs use the same deterministic task stream so the only variable is the sprint policy.
 */
public class SprintingBenchmark {

    private static final int PROCS_PER_SERVER = 2;
    private static final int SERVERS_PER_RACK = 10;
    private static final int NUM_RUNNERS = 40;
    private static final int NUM_EPOCHS = 500;
    private static final int TASKS_PER_EPOCH = 15; // heavy load: ~75 work units/epoch vs 40 runner capacity
    private static final long SEED = 42L;

    static class SimResult {
        double[] workPerEpoch = new double[NUM_EPOCHS];
        int totalSprints = 0;
        int totalPowerTrips = 0;
        double totalSprintBonus = 0;
        int tasksCompleted = 0;
    }

    public static void main(String[] args) {
        int totalTasks = NUM_EPOCHS * TASKS_PER_EPOCH;

        List<Task> bellmanTasks = generateTasks(SEED, totalTasks, 0);
        List<Task> noSprintTasks = generateTasks(SEED, totalTasks, totalTasks);

        // Compute total work fed for reference
        double totalWorkFed = bellmanTasks.stream().mapToDouble(Task::getDuration).sum();

        System.out.println("Running Bellman policy...");
        DataCenter bellmanDC = new DataCenter(PROCS_PER_SERVER, SERVERS_PER_RACK, NUM_RUNNERS, new ArrayList<>());
        SimResult bellman = runSimulation(bellmanDC, bellmanTasks);

        System.out.println("Running no-sprint baseline...");
        DataCenter noSprintDC = new DataCenter(PROCS_PER_SERVER, SERVERS_PER_RACK, NUM_RUNNERS, new ArrayList<>());
        noSprintDC.setFixedThreshold(Double.MAX_VALUE);
        SimResult noSprint = runSimulation(noSprintDC, noSprintTasks);

        printResults(bellman, noSprint, totalWorkFed);
    }

    private static List<Task> generateTasks(long seed, int count, int idOffset) {
        Random rng = new Random(seed);
        List<Task> tasks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int duration = 3 + rng.nextInt(5); // 3-7 epochs
            double utility;
            if (rng.nextDouble() < 0.7) {
                utility = clampedNormal(rng, 0.2, 0.08);
            } else {
                utility = clampedNormal(rng, 0.8, 0.08);
            }
            tasks.add(new Task(idOffset + i, duration, utility));
        }
        return tasks;
    }

    private static double clampedNormal(Random rng, double mean, double std) {
        double x = mean + std * rng.nextGaussian();
        return Math.max(0.0, Math.min(1.0, x));
    }

    private static double totalSystemWork(DataCenter dc) {
        double total = 0;
        for (TaskRunner r : dc.getRunners()) {
            total += r.getTotalWork();
        }
        for (Task t : dc.getTasks()) {
            total += t.getDuration();
        }
        return total;
    }

    private static SimResult runSimulation(DataCenter dc, List<Task> allTasks) {
        SimResult result = new SimResult();
        int taskIdx = 0;

        // Track all tasks ever assigned to count completions
        List<Task> allAssigned = new ArrayList<>();

        for (int epoch = 0; epoch < NUM_EPOCHS; epoch++) {
            List<Task> batch = new ArrayList<>();
            for (int t = 0; t < TASKS_PER_EPOCH && taskIdx < allTasks.size(); t++, taskIdx++) {
                batch.add(allTasks.get(taskIdx));
            }
            dc.addTasks(batch);
            allAssigned.addAll(batch);

            // Snapshot power recovery state before epoch
            boolean[] wasInRecovery = new boolean[NUM_RUNNERS];
            for (int i = 0; i < NUM_RUNNERS; i++) {
                wasInRecovery[i] = dc.getRunners().get(i).isInPowerRecovery();
            }

            double workBefore = totalSystemWork(dc);
            dc.runEpoch();
            double workAfter = totalSystemWork(dc);

            result.workPerEpoch[epoch] = workBefore - workAfter;

            // Count sprinters
            int sprintersThisEpoch = 0;
            for (TaskRunner r : dc.getRunners()) {
                if (r.sprintedThisEpoch()) {
                    sprintersThisEpoch++;
                }
            }
            result.totalSprints += sprintersThisEpoch;

            // Detect new power trips
            for (int i = 0; i < NUM_RUNNERS; i++) {
                if (!wasInRecovery[i] && dc.getRunners().get(i).isInPowerRecovery()) {
                    result.totalPowerTrips++;
                    break;
                }
            }
        }

        // Count completed tasks
        for (Task t : allAssigned) {
            if (t.getState() == TaskState.COMPLETED) {
                result.tasksCompleted++;
            }
        }

        return result;
    }

    private static void printResults(SimResult bellman, SimResult noSprint, double totalWorkFed) {
        double bellmanTotal = 0, noSprintTotal = 0;

        System.out.println();
        System.out.printf("%-8s | %14s | %14s | %14s | %14s%n",
                "Epoch", "Bellman Work", "NoSprint Work", "Bellman Cumul", "NoSprint Cumul");
        System.out.println("-".repeat(73));

        for (int i = 0; i < NUM_EPOCHS; i++) {
            bellmanTotal += bellman.workPerEpoch[i];
            noSprintTotal += noSprint.workPerEpoch[i];

            if (i == 0 || i == NUM_EPOCHS - 1 || (i + 1) % 50 == 0) {
                System.out.printf("%-8d | %14.1f | %14.1f | %14.1f | %14.1f%n",
                        i + 1, bellman.workPerEpoch[i], noSprint.workPerEpoch[i],
                        bellmanTotal, noSprintTotal);
            }
        }

        double additionalWork = bellmanTotal - noSprintTotal;
        double pctImprovement = (noSprintTotal > 0) ? (additionalWork / noSprintTotal) * 100 : 0;

        System.out.println();
        System.out.println("=== RESULTS ===");
        System.out.printf("Total work fed:           %,.1f epoch-units across %d tasks%n",
                totalWorkFed, NUM_EPOCHS * TASKS_PER_EPOCH);
        System.out.println();
        System.out.printf("Bellman total work:       %,.1f%n", bellmanTotal);
        System.out.printf("No-sprint total work:     %,.1f%n", noSprintTotal);
        System.out.printf("Additional work done:     %,.1f (%+.1f%%)%n", additionalWork, pctImprovement);
        System.out.println();
        System.out.printf("Bellman tasks completed:  %,d%n", bellman.tasksCompleted);
        System.out.printf("No-sprint tasks completed:%,d%n", noSprint.tasksCompleted);
        System.out.printf("Additional tasks:         %+d%n", bellman.tasksCompleted - noSprint.tasksCompleted);
        System.out.println();
        System.out.printf("Bellman total sprints:    %,d (%.1f/epoch avg)%n",
                bellman.totalSprints, (double) bellman.totalSprints / NUM_EPOCHS);
        System.out.printf("Bellman power trips:      %d%n", bellman.totalPowerTrips);
        System.out.printf("No-sprint sprints:        %,d (sanity check)%n", noSprint.totalSprints);
    }
}
