package org.sprinting.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sprinting.coordinator.GreedyScheduler;
import org.sprinting.coordinator.SprintCoordinator;
import org.sprinting.hardware.HardwareBridge;
import org.sprinting.hardware.HardwareBridge.ServerState;

/**
 * Represents the data center configuration with # of multiprocessors in a server, 
 * # of servers in a rack, task runners, scheduler, epochSimulator and tasks.
 * Every procsPerServer ids belong to the same server, and every serversPerRack * procsPerServer ids belong to the same rack.
 */

public class DataCenter {
    List<TaskRunner> runners;
    GreedyScheduler scheduler;
    List<Task> tasks;
    private double[] chipTemps;
    private double[] hydrogelStates;
    final int MAX_RACK_SPRINTS = 6;
    SprintCoordinator coordinator; 

    // Hardware bridge for physical demo
    private HardwareBridge hwBridge;
    private int trackedRunnerId = 0;  // which runner drives the physical chip
    private int epochCounter = 0;

    public DataCenter(int procsPerServer, int serversPerRack, int numRunners, List<Task> init_tasks) {
        this.runners = new LinkedList<>();
        this.tasks = init_tasks;
        this.chipTemps = new double[numRunners];
        for (int i = 0; i < numRunners; i++) {
            int serverId = i / procsPerServer;
            int rackId = serverId / serversPerRack;
            runners.add(new TaskRunner(i, 0.75, serverId, rackId));
        }
        scheduler = new GreedyScheduler(runners);
        hydrogelStates = new double[numRunners];
        Arrays.fill(hydrogelStates, 1.0); // start fully charged
        this.coordinator = new SprintCoordinator(10); // thresholds are recomputed every 10 epochs
    }

    /**
     * Initialize the hardware bridge to the Tektronix PWS4323 power supply.
     * Call after constructor.
     *
     * @param dryRun true = no real hardware (logs what would happen), false = live PSU control
     */
    public void initHardware(boolean dryRun) {
        try {
            if (dryRun) {
                hwBridge = new HardwareBridge("python3", "hardware_bridge.py", "--dry-run");
            } else {
                hwBridge = new HardwareBridge("python3", "hardware_bridge.py",
                    "--visa-resource", "USB0::1689::913::C010738::0::INSTR",
                    "--sprint-voltage", "5.0",
                    "--work-voltage",   "2.5",
                    "--idle-voltage",   "0.0",
                    "--current-limit",  "1.0");
            }
            hwBridge.start();
        } catch (Exception e) {
            System.err.println("Hardware bridge failed to start: " + e.getMessage());
            hwBridge = null;
        }
    }

    /**
     * Shut down the hardware bridge and power supply safely.
     */
    public void shutdownHardware() {
        if (hwBridge != null) {
            hwBridge.stop();
            hwBridge = null;
        }
    }

    /**
     * Set which runner ID maps to the physical chip on the demo testbed.
     */
    public void setTrackedRunnerId(int id) {
        if (id >= 0 && id < runners.size()) {
            this.trackedRunnerId = id;
        }
    }

    public int getTrackedRunnerId() {
        return trackedRunnerId;
    }

    public void runEpoch() {
        epochCounter++;
        coordinator.onEpoch(runners);
        int initialTaskCount = tasks.size();
        for (int i = 0; i < initialTaskCount; i++) {
            scheduler.assignTask(tasks.remove(0));
        }

        for (TaskRunner runner : runners) {
            runner.evaluateSprint();
        }

        // Update chip temps and hydrogel states per runner
        for (int i = 0; i < runners.size(); i++) {
            double tempChipTemp = chipTemps[i];
            chipTemps[i] = computeNewTemperature(chipTemps[i],
                runners.get(i).isSprinting(), hydrogelStates[i]);
            // Thermal failure only when chip is at max temp AND hydrogel is depleted
            if (chipTemps[i] >= 1.0 && hydrogelStates[i] == 0.0) {
                runners.get(i).updateEpochsInRecoveryForThermalFailure();
            }
            hydrogelStates[i] = computeNewHydrogelState(tempChipTemp, 
                runners.get(i).isSprinting(), hydrogelStates[i]);
        }
        
        Map<Integer, Integer> sprintersPerRack = new HashMap<>();
        for (TaskRunner runner : runners) {
            if (runner.isSprinting()) {
                sprintersPerRack.merge(runner.getRackId(), 1, Integer::sum);
            }
        }

        for (Map.Entry<Integer, Integer> entry : sprintersPerRack.entrySet()) {
            int rackId = entry.getKey();
            int sprinters = entry.getValue();
        
            if (sprinters > MAX_RACK_SPRINTS) {
                for (TaskRunner runner : runners) {
                    if (runner.getRackId() == rackId) {
                        runner.updateEpochsInRecoveryForPowerFailure();
                    }
                }
            }
        }

        // If no failures then proceed
        for (TaskRunner runner : runners) {
            runner.executeEpoch();
            runner.updateState();
        }

        // --- Hardware: send tracked runner's sprint state to the power supply ---
        if (hwBridge != null && hwBridge.isRunning()) {
            TaskRunner tracked = runners.get(trackedRunnerId);
            ServerState hwState = tracked.isSprinting() ? ServerState.SPRINTING
                                : tracked.isWorking()   ? ServerState.WORKING
                                :                         ServerState.IDLE;
            boolean ack = hwBridge.sendEpoch(epochCounter, hwState);
            if (ack) {
                System.out.printf("[HW] Epoch %d: runner %d -> %s%n",
                    epochCounter, trackedRunnerId, hwState);
            }
        }
    }

    public static double computeNewTemperature(double currentTemp, boolean isSprinting, double hydrogelState) {
        return isSprinting ? Math.min(1.0, currentTemp + 0.25) : Math.max(0.0, currentTemp - 0.05);
    }

    public static double computeNewHydrogelState(double currentTemp, boolean isSprinting, double hydrogelState) {
        if (isSprinting) {
            return Math.max(0.0, hydrogelState - 0.1);
        } else {
            return Math.min(1.0, hydrogelState + 0.05);
        }
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    public void addTasks(List<Task> newTasks) {
        if (newTasks != null && !newTasks.isEmpty()) {
            tasks.addAll(newTasks);
        }
    }

    public List<TaskRunner> getRunners() {
        return runners;
    }

    public double[] getHydrogelStates() {
        return hydrogelStates;
    }

    public double[] getChipTemps() {
        return chipTemps;
    }

    public double getChipTemp(int runnerId) {
        if (runnerId >= 0 && runnerId < chipTemps.length) {
            return chipTemps[runnerId];
        }
        return 0.0;
    }

    public double getHydrogelState(int runnerId) {
        if (runnerId >= 0 && runnerId < hydrogelStates.length) {
            return hydrogelStates[runnerId];
        }
        return 0.0;
    }

    public double getCurrentThreshold() {
        return coordinator.getCurrentThreshold();
    }

    public int getEpochsUntilRecompute() {
        return coordinator.getEpochsUntilRecompute();
    }

    public int getEpochCounter() {
        return epochCounter;
    }
}