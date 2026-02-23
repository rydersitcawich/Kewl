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

        int runnersPerRack = 10; // matches your SERVERS_PER_RACK * PROCS_PER_SERVER
        params.N    = runnersPerRack;
        params.Nmin = 2;   // ~25% of 10, paper uses Nmin = 0.25*N
        params.Nmax = 6;   // matches your MAX_RACK_SPRINTS
    
        double[] utilities = runners.stream()
            .mapToDouble(r -> r.getCurrentUtility())
            .filter(u -> u > 0)
            .toArray();
    
        if (utilities.length == 0) return;
    
        double mean = 0, variance = 0;
        for (double u : utilities) mean += u;
        mean /= utilities.length;
        for (double u : utilities) variance += (u - mean) * (u - mean);
        double std = Math.sqrt(variance / utilities.length);
        std = Math.max(std, 0.01);
    
        params.uMin = 0.0;
        params.uMax = 1.0;
    
        SprintingBellmanDemo.UtilityDistribution dist =
            new SprintingBellmanDemo.NarrowGaussian(mean, std, params.uMin, params.uMax);
    
        SprintingBellmanDemo.BellmanMeanFieldSolver solver =
            new SprintingBellmanDemo.BellmanMeanFieldSolver(params, dist);
    
        SprintingBellmanDemo.Result result =
            solver.solve(0.40, 200, 2000, 1e-6, 1e-8);
    
        System.out.printf("Recomputed: u_T*=%.4f, P_trip=%.4f, nS=%.2f, converged=%s%n",
            result.thresholdUT, result.ptrip, result.expectedNSprinters, result.converged);
    
            double rawThreshold = result.thresholdUT;
            double normalizedThreshold = rawThreshold / params.uMax;
            
            // Clamp to a sensible range so it always sits between the two modes
            double finalThreshold = Math.max(0.4, Math.min(0.6, normalizedThreshold));
            
            System.out.printf("Raw u_T*=%.4f → final threshold=%.4f%n", rawThreshold, finalThreshold);
            
            this.currentThreshold = finalThreshold;
            for (TaskRunner runner : runners) {
                runner.setSprintThreshold(finalThreshold);
            }
    }

    public double getCurrentThreshold() {
        return currentThreshold;
    }

    public int getEpochsUntilRecompute() {
        return recomputeInterval - epochsSinceLastRecompute;
    }
}