package org.sprinting.model;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

/**
 * SprintingBellmanDemo
 *
 * Implements the Bellman equations and mean-field fixed point from:
 * "The Computational Sprinting Game" (ASPLOS’16).
 *
 * Equations/structure used:
 *  - Bellman Eq. (1)–(4): V(u,A), VS, V¬S, and V(A)=∫V(u,A)f(u)du
 *  - Cooling/Recovery values Eq. (5)–(6) (closed form used in code)
 *  - Threshold Eq. (8): uT = δ (V(A) - V(C)) (1 - Ptrip)
 *  - Sprint prob and sprinter count Eq. (9)–(11), with trip-curve stencil
 *
 * Defaults match Table 2 (Nmin=250, Nmax=750, pc=0.50, pr=0.88, δ=0.99, N=1000).
 *
 * CLI:
 *   java SprintingBellmanDemo              # bimodal "PageRank-like" utility distribution
 *   java SprintingBellmanDemo linear       # narrow "LinearRegression-like" utility distribution
 */
public class SprintingBellmanDemo {

    // ==== Parameters from the paper (Table 2) and text ====
    public static class Params {
        public int N = 1000;                 // number of agents in rack
        public int Nmin = 250;               // trip curve lower knee (Fig. 3)
        public int Nmax = 750;               // trip curve upper knee (Fig. 3)
        public double pc = 0.50;             // P(stay cooling)  -> Δt_cool ≈ 1/(1-pc)
        public double pr = 0.88;             // P(stay recovery) -> Δt_rec  ≈ 1/(1-pr)
        public double delta = 0.99;          // discount factor
        // Utility domain [uMin, uMax] (normalized TPS-like)
        public double uMin = 0.0;
        public double uMax = 1.0;           // allows >10× spikes
        public int gridU = 800;              // discretization for u ∈ [uMin, uMax]
    }

    // ==== Utility distribution f(u) ====
    public interface UtilityDistribution {
        double pdf(double u);
        double uMin();
        double uMax();
        default double normalize(int steps) {
            double sum = 0, du = (uMax() - uMin()) / (steps - 1);
            for (int i = 0; i < steps; i++) {
                double u = uMin() + i * du;
                sum += pdf(u) * du;
            }
            return sum;
        }
    }

    // Narrow Gaussian-ish profile (LinearRegression-like)
    public static class NarrowGaussian implements UtilityDistribution {
        final double mu, sigma, uMin, uMax;
        public NarrowGaussian(double mu, double sigma, double uMin, double uMax) {
            this.mu = mu; this.sigma = sigma; this.uMin = uMin; this.uMax = uMax;
        }
        public double pdf(double u) {
            if (u < uMin || u > uMax) return 0.0;
            double z = (u - mu) / sigma;
            return Math.exp(-0.5 * z * z) / (sigma * Math.sqrt(2 * Math.PI));
        }
        public double uMin() { return uMin; }
        public double uMax() { return uMax; }
    }

    // Bimodal Gaussian mixture (PageRank-like)
    public static class BimodalGaussian implements UtilityDistribution {
        final double mu1, s1, w1, mu2, s2, w2, uMin, uMax;
        public BimodalGaussian(double mu1, double s1, double w1,
                               double mu2, double s2, double w2,
                               double uMin, double uMax) {
            this.mu1 = mu1; this.s1 = s1; this.w1 = w1;
            this.mu2 = mu2; this.s2 = s2; this.w2 = w2;
            this.uMin = uMin; this.uMax = uMax;
        }
        private double gauss(double u, double mu, double s) {
            double z = (u - mu) / s;
            return Math.exp(-0.5 * z * z) / (s * Math.sqrt(2 * Math.PI));
        }
        public double pdf(double u) {
            if (u < uMin || u > uMax) return 0.0;
            double g1 = gauss(u, mu1, s1);
            double g2 = gauss(u, mu2, s2);
            return Math.max(0.0, w1 * g1 + w2 * g2);
        }
        public double uMin() { return uMin; }
        public double uMax() { return uMax; }
    }

    // ==== Solver for the nested fixed points (Bellman + mean-field) ====
    public static class Result {
        public boolean converged;
        int outerIters;
        public double ptrip;
        public double thresholdUT;
        double pSprint;
        double pActive;
        public double expectedNSprinters;
        double V_A, V_C, V_R;
    }

    public static class BellmanMeanFieldSolver {
        final Params P;
        final UtilityDistribution dist;
        final double[] gridU;
        final double du;

        public BellmanMeanFieldSolver(Params p, UtilityDistribution d) {
            this.P = p; this.dist = d;
            this.gridU = new double[p.gridU];
            this.du = (p.uMax - p.uMin) / (p.gridU - 1);
            for (int i = 0; i < p.gridU; i++) gridU[i] = p.uMin + i * du;
        }

        public Result solve(double ptripInit, int maxOuter, int maxInner, double tolOuter, double tolInner) {
            double ptrip = clamp(ptripInit, 0.0, 1.0);
            double V_A = 0.0;
            double[] V_A_of_u = new double[P.gridU];

            Result r = new Result();

            for (int outer = 0; outer < maxOuter; outer++) {
                double VA = V_A;
                for (int inner = 0; inner < maxInner; inner++) {
                    // Closed-form V(R) and V(C)
                    double denomR = 1.0 - P.delta * P.pr;
                    double V_R = (P.delta * (1.0 - P.pr) * VA) / denomR;

                    double denomC = 1.0 - P.delta * (1.0 - ptrip) * P.pc;
                    double V_C = (P.delta * ((1.0 - ptrip) * (1.0 - P.pc) * VA + ptrip * V_R)) / denomC;

                    // Update V(u,A) pointwise by max{VS, V¬S}
                    for (int i = 0; i < P.gridU; i++) {
                        double u = gridU[i];
                        double VS = u + P.delta * ((1.0 - ptrip) * V_C + ptrip * V_R);
                        double VnS = P.delta * ((1.0 - ptrip) * VA + ptrip * V_R);
                        V_A_of_u[i] = Math.max(VS, VnS);
                    }

                    // New expected V(A) = ∫ V(u,A) f(u) du
                    double VAnew = integrate(u -> V_A_of_u[idx(u)] * dist.pdf(u));
                    if (Math.abs(VAnew - VA) < tolInner) {
                        V_A = VAnew;

                        double denomR2 = 1.0 - P.delta * P.pr;
                        double V_R2 = (P.delta * (1.0 - P.pr) * V_A) / denomR2;
                        double denomC2 = 1.0 - P.delta * (1.0 - ptrip) * P.pc;
                        double V_C2 = (P.delta * ((1.0 - ptrip) * (1.0 - P.pc) * V_A + ptrip * V_R2)) / denomC2;

                        double uT = P.delta * (V_A - V_C2) * (1.0 - ptrip);
                        double pSprint = integrate(u -> (u >= uT ? 1.0 : 0.0) * dist.pdf(u));
                        pSprint = clamp(pSprint, 0.0, 1.0);

                        double pActive = (1.0 - P.pc) / (1.0 + pSprint - P.pc);
                        pActive = clamp(pActive, 0.0, 1.0);

                        double nS = pSprint * pActive * P.N;

                        double ptripNew = tripCurve(nS, P.Nmin, P.Nmax);

                        if (Math.abs(ptripNew - ptrip) < tolOuter) {
                            r.converged = true;
                            r.outerIters = outer + 1;
                            r.ptrip = ptripNew;
                            r.thresholdUT = uT;
                            r.pSprint = pSprint;
                            r.pActive = pActive;
                            r.expectedNSprinters = nS;
                            r.V_A = V_A;
                            r.V_C = V_C2;
                            r.V_R = V_R2;
                            return r;
                        }

                        ptrip = ptripNew;
                        break;
                    }
                    VA = VAnew;
                }
            }

            // Not converged
            r.converged = false;
            r.outerIters = maxOuter;
            r.ptrip = ptrip;
            double denomR = 1.0 - P.delta * P.pr;
            r.V_R = (P.delta * (1.0 - P.pr) * V_A) / denomR;
            double denomC = 1.0 - P.delta * (1.0 - ptrip) * P.pc;
            r.V_C = (P.delta * ((1.0 - ptrip) * (1.0 - P.pc) * V_A + ptrip * r.V_R)) / denomC;
            r.V_A = V_A;
            r.thresholdUT = P.delta * (r.V_A - r.V_C) * (1.0 - ptrip);
            r.pSprint = integrate(u -> (u >= r.thresholdUT ? 1.0 : 0.0) * dist.pdf(u));
            r.pActive = (1.0 - P.pc) / (1.0 + r.pSprint - P.pc);
            r.expectedNSprinters = r.pSprint * r.pActive * P.N;
            return r;
        }

        // Numeric ∫ g(u) du over [uMin, uMax] using the solver grid
        private double integrate(DoubleUnaryOperator g) {
            double sum = 0.0;
            for (int i = 0; i < P.gridU; i++) {
                double u = gridU[i];
                double w = (i == 0 || i == P.gridU - 1) ? 0.5 : 1.0; // trapezoid endpoints
                sum += w * g.applyAsDouble(u);
            }
            return sum * du;
        }

        private double tripCurve(double nS, int Nmin, int Nmax) {
            if (nS < Nmin) return 0.0;
            if (nS > Nmax) return 1.0;
            if (Nmax == Nmin) return 1.0;
            return (nS - Nmin) / (double)(Nmax - Nmin);
        }

        private int idx(double u) {
            int i = (int)Math.round((u - P.uMin) / du);
            if (i < 0) i = 0;
            if (i >= P.gridU) i = P.gridU - 1;
            return i;
        }

        private static double clamp(double x, double lo, double hi) {
            return Math.max(lo, Math.min(hi, x));
        }
    }

    // ==== Main ====
    public static void main(String[] args) {
        Params p = new Params();
        UtilityDistribution dist;
        String mode = (args.length > 0) ? args[0].trim().toLowerCase() : "pagerank";

        if ("linear".equals(mode) || "lr".equals(mode)) {
            dist = new NarrowGaussian(4.0, 0.6, p.uMin, p.uMax);
        } else {
            dist = new BimodalGaussian(
                    3.0, 0.8, 0.70,
                    10.0, 1.2, 0.30,
                    p.uMin, p.uMax);
        }

        BellmanMeanFieldSolver solver = new BellmanMeanFieldSolver(p, dist);
        Result r = solver.solve(/*Ptrip init*/0.40, /*maxOuter*/200, /*maxInner*/2000,
                /*tolOuter*/1e-6, /*tolInner*/1e-8);

        // ===== Terminal output =====
        banner("Computational Sprinting Bellman Solver");
        System.out.printf(Locale.US, "Profile: %s\n", ("linear".equals(mode) ? "LinearRegression-like (narrow)" :
                "PageRank-like (bimodal)"));
        System.out.printf(Locale.US, "Params: N=%d, Nmin=%d, Nmax=%d, pc=%.2f, pr=%.2f, δ=%.2f\n",
                p.N, p.Nmin, p.Nmax, p.pc, p.pr, p.delta);

        System.out.println();
        System.out.println("== Mean-field balancing ==");
        System.out.printf(Locale.US, "Converged: %s  (outer iters: %d)\n", r.converged ? "YES" : "NO", r.outerIters);
        System.out.printf(Locale.US, "P_trip*: %.6f\n", r.ptrip);
        System.out.printf(Locale.US, "u_T* (threshold): %.6f\n", r.thresholdUT);
        System.out.printf(Locale.US, "Prob[sprint] p_s: %.6f\n", r.pSprint);
        System.out.printf(Locale.US, "Prob[active] p_A: %.6f\n", r.pActive);
        System.out.printf(Locale.US, "E[# sprinters] n_S: %.2f (Nmin=%d, Nmax=%d)\n", r.expectedNSprinters, p.Nmin, p.Nmax);
        System.out.println(thermometer(r.expectedNSprinters, p.Nmin, p.Nmax, p.N));
        System.out.printf(Locale.US, "Values: V(A)=%.6f  V(C)=%.6f  V(R)=%.6f\n", r.V_A, r.V_C, r.V_R);
        System.out.println("\nInterpretation:");
        System.out.println(" - If u (instant sprint utility) >= u_T*, sprint; else wait.");
        System.out.println(" - n_S near Nmin indicates agents self-balance just below the breaker’s tolerance band.");
    }

    private static void banner(String title) {
        String line = repeat("═", title.length() + 2);
        System.out.println("╔" + line + "╗");
        System.out.println("║ " + title + " ║");
        System.out.println("╚" + line + "╝");
    }

    // Java 8 compatible repeat helper (since String.repeat is Java 11+)
    private static String repeat(String s, int n) {
        if (n <= 0) return "";
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }

    private static String thermometer(double nS, int Nmin, int Nmax, int N) {
        int width = 40;
        int pos = (int)Math.round(width * nS / N);
        int posMin = (int)Math.round(width * Nmin / (double)N);
        int posMax = (int)Math.round(width * Nmax / (double)N);
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i <= width; i++) {
            if (i == pos) sb.append("|");            // current expected sprinters
            else if (i == posMin || i == posMax) sb.append("^"); // Nmin/Nmax markers
            else sb.append("-");
        }
        sb.append("]  (‘|’=n_S, ‘^’=Nmin/Nmax)");
        return sb.toString();
    }
}
