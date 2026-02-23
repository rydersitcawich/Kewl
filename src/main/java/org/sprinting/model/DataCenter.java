package org.sprinting.model;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sprinting.coordinator.GreedyScheduler;
import org.sprinting.coordinator.SprintCoordinator;

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
        this.coordinator = new SprintCoordinator(10); // thresholds are recomputed every 30 epochs
    }

    public void runEpoch() {
        coordinator.onEpoch(runners);
        int initialTaskCount = tasks.size();
        for (int i = 0; i < initialTaskCount; i++) {
            scheduler.assignTask(tasks.remove(0));
        }

        for (TaskRunner runner : runners) {
            runner.evaluateSprint();
        }

        // Map<Integer, Integer> sprintersPerServer = new HashMap<>();
        // for (TaskRunner runner : runners) {
        //     if (runner.isSprinting()) {
        //         sprintersPerServer.merge(runner.getServerId(), 1, Integer::sum);
        //     }
        // }

        // for (Map.Entry<Integer, Double> entry : serverTemps.entrySet()) {
        //     int serverId = entry.getKey();
        //     double temp = entry.getValue();
        //     double hydrogelState = hydrogelStates[serverId];
        //     int sprinters = sprintersPerServer.getOrDefault(serverId, 0);

        //     temp = computeNewTemperature(temp, sprinters, hydrogelState);
        //     temp = Math.max(0.0, Math.min(1.0, temp));
        //     serverTemps.put(serverId, temp);

        //     hydrogelState = computeNewHydrogelState(hydrogelState, sprinters, hydrogelState);
        //     hydrogelStates[serverId] = hydrogelState;

        //     if (temp >= 1.0) {
        //         for (TaskRunner runner : runners) {
        //             if (runner.getServerId() == serverId) {
        //                 runner.updateEpochsInRecoveryForThermalFailure();
        //             }
        //         }
        //         // System.out.println("Server " + serverId + " overheated! All runners cooling.");
        //     }
        // }
        //need to update chiptemps
        for (int i = 0; i < runners.size(); i++) {
            double tempChipTemp = chipTemps[i];
            chipTemps[i] = computeNewTemperature(chipTemps[i], 
                runners.get(i).isSprinting(), hydrogelStates[i]);
            if (chipTemps[i] == 1.0) {
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
                // System.out.println("Rack " + rackId + " exceeded power limit! All runners recovering.");
            }
        }
        // if no failures then proceed
        for (TaskRunner runner : runners) {
            runner.executeEpoch();
            runner.updateState();
        }
    }

    public static double computeNewTemperature(double currentTemp, boolean isSprinting, double hydrogelState) {
        if (hydrogelState > 0) {
            return currentTemp;
        }
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

}