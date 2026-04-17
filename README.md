# Computational Sprinting Data Center Simulator
## Setup
1. mvn clean compile 
2. mvn javafx:run    


## Overview
This project simulates a data center environment with computational sprinting, implementing the Bellman equilibrium threshold policy from Fan et al. (ASPLOS '16). The simulation models multiprocessors (TaskRunners) distributed across servers and racks, executing tasks while dynamically computing optimal sprint thresholds using mean-field game theory. A JavaFX GUI provides real-time visualization, and a hardware bridge can drive a real Tektronix PWS4323 power supply to physically demonstrate sprinting states.

Key features include:
* Bellman equilibrium sprinting: A SprintCoordinator recomputes the optimal sprint threshold every N epochs by fitting a bimodal Gaussian to current task utilities and solving the Bellman fixed-point equations.
* Chip-level thermal modeling: Each processor accumulates temperature based on sprinting activity. Overheating triggers cooling recovery for the affected TaskRunner.
* Rack-level power limits: Excessive simultaneous sprinting in a rack triggers power recovery across all TaskRunners in the rack. A circuit breaker (Nmin/Nmax) prevents thundering herd scenarios.
* Dynamic task assignment: Tasks are scheduled using a greedy scheduler, which assigns new tasks to the least-loaded runner.
* Hardware-in-the-loop: A Python bridge controls a Tektronix PWS4323 power supply over USB/VISA, mapping runner states (sprinting/working/idle) to voltage setpoints in real time.
* JavaFX GUI: Real-time visualization with play/pause/step controls, live sprint threshold display, recompute countdown, hydrogel state, and hierarchical rack/server/processor views.

## Components
### DataCenter
* Represents the full datacenter with configurable servers per rack and processors per server.
* Tracks server temperatures and rack-level sprinter counts.
* Handles task assignment, sprint evaluation, and epoch progression.
* Detects thermal and power failures and applies recovery to affected TaskRunners.
* Integrates with SprintCoordinator for dynamic threshold updates and HardwareBridge for PSU control.
### TaskRunner
* Models a single processor capable of executing tasks.
* Can enter a sprinting state for accelerated execution when task utility exceeds the current Bellman threshold.
* Tracks epochs of recovery for thermal or power failures.
* Associated with a server ID and rack ID for hierarchical failure handling.
### Task
* Represents a unit of work with a specified duration in epochs.
* Carries a utility value drawn from a bimodal distribution (low/high utility tasks).
* Can execute faster if the runner is sprinting.
* Tracks its current state (PENDING, RUNNING, COMPLETED).
### SprintCoordinator
* Periodically recomputes the optimal sprint threshold using the Bellman mean-field solver.
* Fits a bimodal Gaussian mixture to the current task utility distribution.
* Broadcasts updated thresholds to all TaskRunners.
### SprintingBellmanDemo
* Implements the Bellman equations (1)–(4) and threshold equation (8) from Fan et al.
* Solves the mean-field fixed-point to find the equilibrium threshold and trip probability.
### HardwareBridge
* Spawns a Python subprocess (`hardware_bridge.py`) to control a Tektronix PWS4323 power supply.
* Sends per-epoch JSON commands with runner state; receives acknowledgements.
* Supports dry-run mode for testing without hardware.
### GreedyScheduler
* Assigns tasks to the least-loaded runner at the moment of assignment.
* Ensures tasks are balanced across all runners.
### GUI (DataCenterGUI)
* JavaFX application with play/pause/step controls and adjustable simulation speed.
* Displays live sprint threshold, next recompute countdown, and hydrogel charge state.
* Hierarchical visualization of racks, servers, and processors color-coded by state.
* Event log and dynamic task injection.
## Simulation Flow
* Initialize DataCenter: Specify processors per server, servers per rack, number of TaskRunners, and initial tasks.
* Assign tasks: Either at initialization or dynamically using addTask(s).
* Run epochs:
 1. SprintCoordinator recomputes threshold (if recompute interval reached).
 2. Evaluate sprinting for each TaskRunner against current threshold.
 3. Update server temperatures and check for thermal failures.
 4. Check rack sprinter counts and enforce power limits via circuit breaker.
 5. Execute task epochs and update recovery states.
 6. Send current state to hardware bridge (if enabled).
## Notes
* Temperature Model: Each chip tracks a simplified temperature (0–1). Sprinting increases temperature; cooldown reduces it. Thermal failures occur at temperature >= 1.
* Power Model: Racks have a maximum allowed number of sprinters (MAX_RACK_SPRINTS). A circuit breaker with Nmin/Nmax bounds prevents thundering herd oscillation.
* Recovery: Epoch-based counters control recovery from thermal or power failures. Sprinting is disabled during recovery.
* Hydrogel Model: Runners track a hydrogel cooldown state — sprinting depletes the charge, which recovers over idle/working epochs.

## Benchmark: Bellman Equilibrium vs Never-Sprint Baseline

Compares the Bellman equilibrium threshold policy (E-T) against a never-sprint baseline over 1000 epochs using identical deterministic task streams, at paper scale (Fan et al., ASPLOS'16).

### Experiment Setup
* **Runners:** 1,000 processors in 1 rack (2 procs/server, 500 servers/rack), matching the paper's N=1000
* **Task load:** 375 tasks/epoch (heavy load — ~1,875 work-units/epoch arriving vs 1,000 base capacity)
* **Task durations:** 3–7 epochs (uniform), seeded RNG for reproducibility
* **Utility distribution:** Bimodal — 50% low-utility tasks (mean=0.15, std=0.10), 50% high-utility tasks (mean=0.75, std=0.15)
* **Sprint multiplier:** `1 + utility * (MAX_SPEEDUP - 1)` with `MAX_SPEEDUP = 4.0` (conservative midpoint of the paper's reported 2–7x realized speedup range)
* **Circuit breaker:** Nmin=250 (25% of N), Nmax=750 (75% of N), matching paper Table 2
* **Hydrogel cooldown:** 4 epochs working, 3 epochs idle to fully recharge
* **Rack recovery:** 8 epochs after power trip
* **Bellman recompute interval:** Every 10 epochs, fitting a bimodal Gaussian to current task utilities
* **Never-sprint baseline:** Sprint threshold set to `Double.MAX_VALUE`, coordinator disabled

### Run

```
mvn exec:java -Dexec.mainClass=org.sprinting.benchmark.SprintingBenchmark
```

### Results

* +16.4% additional effective work (1,162,984 vs 999,125 epoch-units)
* +32,819 additional tasks completed (232,219 vs 199,400 — +16.5%)
* 109,500 total sprints across 1,000 epochs (109.5 sprints/epoch avg)
* 0 circuit breaker trips