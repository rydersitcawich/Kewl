package org.sprinting.coordinator;

import org.sprinting.model.TaskRunner;
import org.sprinting.model.SprintingBellmanDemo;
import java.util.Arrays;
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
        // Match simulation's recovery rates
        this.params.pc = 0.667;  // 3-epoch working cooldown: 1/(1-pc) = 3
        this.params.pr = 0.88;   // 8-epoch rack recovery (paper Table 2)
    }

    public void onEpoch(List<TaskRunner> runners) {
        epochsSinceLastRecompute++;
        if (epochsSinceLastRecompute >= recomputeInterval) {
            recomputeThresholds(runners);
            epochsSinceLastRecompute = 0;
        }
    }

    private void recomputeThresholds(List<TaskRunner> runners) {

        int runnersPerRack = 20; // SERVERS_PER_RACK(10) * PROCS_PER_SERVER(2) = 20
        params.N    = runnersPerRack;
        params.Nmin = 4;   // 20% of N=20, breaker starts tripping
        params.Nmax = 10;  // 50% of N=20, breaker guaranteed trip
    
        double[] utilities = runners.stream()
            .mapToDouble(r -> r.getCurrentUtility())
            .filter(u -> u > 0)
            .toArray();
    
        if (utilities.length == 0) return;
    
        params.uMin = 0.0;
        params.uMax = 1.0;

        // Split at 0.5 to fit a 2-component GMM matching the bimodal task distribution.
        // Using a single NarrowGaussian over-estimates pSprint between the modes, which
        // drives ptrip → 1 and collapses the threshold to 0 (sprint-everything death spiral).
        double[] low  = Arrays.stream(utilities).filter(u -> u <  0.5).toArray();
        double[] high = Arrays.stream(utilities).filter(u -> u >= 0.5).toArray();

        SprintingBellmanDemo.UtilityDistribution dist;
        if (low.length >= 5 && high.length >= 5) {
            double w1 = (double) low.length  / utilities.length;
            double w2 = (double) high.length / utilities.length;
            double mu1 = mean(low),  s1 = Math.max(std(low),  0.01);
            double mu2 = mean(high), s2 = Math.max(std(high), 0.01);
            dist = new SprintingBellmanDemo.BimodalGaussian(mu1, s1, w1, mu2, s2, w2, params.uMin, params.uMax);
        } else {
            // Not enough samples in one cluster — fall back to single Gaussian
            double mu = mean(utilities);
            double s  = Math.max(std(utilities), 0.01);
            dist = new SprintingBellmanDemo.NarrowGaussian(mu, s, params.uMin, params.uMax);
        }
    
        SprintingBellmanDemo.BellmanMeanFieldSolver solver =
            new SprintingBellmanDemo.BellmanMeanFieldSolver(params, dist);
    
        SprintingBellmanDemo.Result result =
            solver.solve(0.40, 200, 2000, 1e-6, 1e-8);
    
        System.out.printf("Recomputed: u_T*=%.4f, P_trip=%.4f, nS=%.2f, converged=%s%n",
            result.thresholdUT, result.ptrip, result.expectedNSprinters, result.converged);
    
            double rawThreshold = result.thresholdUT;
            double normalizedThreshold = rawThreshold / params.uMax;
            
            // Sanity-check bounds only — root causes fixed, no longer need hard clamp
            double finalThreshold = Math.max(0.05, Math.min(0.95, normalizedThreshold));
            
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

    private static double mean(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return s / a.length;
    }

    private static double std(double[] a) {
        double m = mean(a), s = 0;
        for (double v : a) s += (v - m) * (v - m);
        return Math.sqrt(s / a.length);
    }
}