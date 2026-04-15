# Computational Sprinting Data Center Simulator
## Setup
1. mvn clean compile 
2. mvn javafx:run    


## Overview
This project simulates a data center environment with computational sprinting. The simulation models multiprocessors (TaskRunners) distributed across servers and racks, executing tasks while respecting thermal and power constraints. There is also a GUI to provide a visual representation of this model.

Key features include:
* Task execution with sprinting: Each TaskRunner can “sprint” to accelerate task completion, subject to thresholds and recovery periods.
* Server-level thermal modeling: Servers accumulate temperature based on the number of sprinters. Overheated servers trigger cooling recovery for all associated TaskRunners.
* Rack-level power limits: Excessive simultaneous sprinting in a rack triggers power recovery across all TaskRunners in the rack.
* Dynamic task assignment: Tasks are scheduled using a greedy scheduler, which assigns new tasks to the least-loaded runner.
* Flexible simulation: Tasks can be added dynamically at any point during simulation.
This setup provides a platform to experiment with computational sprinting strategies, resource contention, and recovery mechanisms in a simplified, yet structured, datacenter environment.
* GUI for visual representation

## Components
### DataCenter
* Represents the full datacenter with configurable servers per rack and processors per server.
* Tracks server temperatures and rack-level sprinter counts.
* Handles task assignment, sprint evaluation, and epoch progression.
* Detects thermal and power failures and applies recovery to affected TaskRunners.
### TaskRunner
* Models a single processor capable of executing tasks.
* Can enter a sprinting state for accelerated execution.
* Tracks epochs of recovery for thermal or power failures.
* Associated with a server ID and rack ID for hierarchical failure handling.
### Task
* Represents a unit of work with a specified duration in epochs.
* Can execute faster if the runner is sprinting.
* Tracks its current state (PENDING, RUNNING, COMPLETED).
### GreedyScheduler
* Assigns tasks to the least-loaded runner at the moment of assignment.
* Ensures tasks are balanced across all runners.
### Main
* Demonstrates how to initialize a DataCenter, add tasks, and run multiple epochs.
* Prints runner states, server temperatures, and failure events for observation.
## Simulation Flow
* Initialize DataCenter: Specify processors per server, servers per rack, number of TaskRunners, and initial tasks.
* Assign tasks: Either at initialization or dynamically using addTask(s).
* Run epochs:
 1. Evaluate sprinting for each TaskRunner.
 2. Update server temperatures and check for thermal failures.
 3. Check rack sprinter counts and enforce power limits.
 4. Execute task epochs and update recovery states.
5.  Observe output: Monitor runner states, server temperatures, and power/thermal recovery events.
## Notes
* Temperature Model: Servers track a simplified temperature (0–1). Sprinting increases temperature; cooldown reduces it. Thermal failures occur at temperature >= 1.
* Power Model: Racks have a maximum allowed number of sprinters (MAX_RACK_SPRINTS). Exceeding this limit triggers rack-wide power recovery.
* Recovery: Epoch-based counters control recovery from thermal or power failures. Sprinting is disabled during recovery.

## Benchmark: Bellman Equilibrium vs Never-Sprint Baseline

Compares the Bellman equilibrium threshold policy (E-T) against a never-sprint baseline over 500 epochs using identical deterministic task streams.

### Experiment Setup
* **Runners:** 40 processors across 2 racks (2 procs/server, 10 servers/rack)
* **Task load:** 15 tasks/epoch (heavy load — 75 work-units/epoch arriving vs 40 base capacity)
* **Task durations:** 3–7 epochs (uniform), seeded RNG for reproducibility
* **Utility distribution:** Bimodal — 70% low-utility tasks (mean=0.2, std=0.08), 30% high-utility tasks (mean=0.8, std=0.08)
* **Sprint multiplier:** `1 + utility * (MAX_SPEEDUP - 1)` with `MAX_SPEEDUP = 4.0` (conservative midpoint of the paper's reported 2–7x realized speedup range)
* **Circuit breaker:** Nmin=4 (20% of rack), Nmax=9 (45% of rack)
* **Hydrogel cooldown:** 4 epochs working, 3 epochs idle to fully recharge
* **Rack recovery:** 8 epochs after power trip
* **Bellman recompute interval:** Every 10 epochs, fitting a bimodal Gaussian to current task utilities
* **Never-sprint baseline:** Sprint threshold set to `Double.MAX_VALUE`, coordinator disabled

### Run

```
mvn exec:java -Dexec.mainClass=org.sprinting.benchmark.SprintingBenchmark
```

### Results

* +11.1% additional effective work (22,172 vs 19,965 epoch-units)
* +446 additional tasks completed (4,411 vs 3,965 — +11.2%)
* 1,517 total sprints across 500 epochs (3.0 sprints/epoch avg)
* 12 circuit breaker trips