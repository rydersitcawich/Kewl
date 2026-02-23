package org.sprinting.coordinator;

import org.sprinting.model.TaskRunner;
import org.sprinting.model.SprintingBellmanDemo;
import java.util.List;

public class SprintCoordinator {

    private final int recomputeInterval; // N epochs
    private int epochsSinceLastRecompute = 0;
    private double currentThreshold = 0.75;

    // Bellman solver parameters — match Table 2
    private final SprintingBellmanDemo.Params params;

    public SprintCoordinator(int recomputeInterval) {
        this.recomputeInterval = recomputeInterval;
        this.params = new SprintingBellmanDemo.Params();
    }

    public void onEpoch(List<TaskRunner> runners) {
        epochsSinceLastRecompute++;
        if (epochsSinceLastRecompute >= recomputeInterval) {
            recomputeThresholds(runners);
            epochsSinceLastRecompute = 0;
        }
    }

    private void recomputeThresholds(List<TaskRunner> runners) {
        // Build empirical f(u) from current task utilities across all runners

        double[] utilities = runners.stream()
            .mapToDouble(r -> r.getCurrentUtility()) // add this getter to TaskRunner
            .filter(u -> u > 0)
            .toArray();

        if (utilities.length == 0) return;

        double mean = 0, variance = 0;
        for (double u : utilities) mean += u;
        mean /= utilities.length;
        for (double u : utilities) variance += (u - mean) * (u - mean);
        double std = Math.sqrt(variance / utilities.length);
        std = Math.max(std, 0.01); // avoid degenerate distribution

        SprintingBellmanDemo.UtilityDistribution dist =
            new SprintingBellmanDemo.NarrowGaussian(mean, std, params.uMin, params.uMax);

        SprintingBellmanDemo.BellmanMeanFieldSolver solver =
            new SprintingBellmanDemo.BellmanMeanFieldSolver(params, dist);

        SprintingBellmanDemo.Result result =
            solver.solve(0.40, 200, 2000, 1e-6, 1e-8);

        double newThreshold = result.thresholdUT;
        System.out.printf("Recomputed threshold: u_T* = %.4f (converged: %s)%n",
            newThreshold, result.converged);

        // Push threshold to all runners
        for (TaskRunner runner : runners) {
            runner.setSprintThreshold(newThreshold);
        }
        this.currentThreshold = newThreshold;
    }

    public double getCurrentThreshold() {
        return currentThreshold;
    }

    public int getEpochsUntilRecompute() {
        return recomputeInterval - epochsSinceLastRecompute;
    }
}