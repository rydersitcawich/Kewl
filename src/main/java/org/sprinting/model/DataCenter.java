package main.java.org.sprinting.model;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.java.org.sprinting.coordinator.GreedyScheduler;

/**
 * Represents the data center configuration with # of multiprocessors in a server, 
 * # of servers in a rack, task runners, scheduler, epochSimulator and tasks.
 * Every procsPerServer ids belong to the same server, and every serversPerRack * procsPerServer ids belong to the same rack.
 */

public class DataCenter {
    private int procsPerServer; // internet says 2 is a good default
    private int serversPerRack; // internet says 20 is a good default
    List<TaskRunner> runners;
    GreedyScheduler scheduler;
    List<Task> tasks;
    private Map<Integer, Double> serverTemps; // Map of server ID to current temperature (0 = cold, 1 = overheated)
    private double[] hydrogelStates; // hydrogel states per server ranging from 0.0 to 1.0 (0.0 = no sprinting equillibrium, 1.0 = fully unsaturated)
    final int MAX_RACK_SPRINTS = 10;

    public DataCenter(int procsPerServer, int serversPerRack, int numRunners, List<Task> init_tasks) {
        this.procsPerServer = procsPerServer;
        this.serversPerRack = serversPerRack;
        this.runners = new LinkedList<>();
        this.tasks = init_tasks;
        this.serverTemps = new HashMap<>();
        for (int i = 0; i < numRunners; i++) {
            int serverId = i / procsPerServer;
            int rackId = serverId / serversPerRack;
            serverTemps.putIfAbsent(serverId, 0.0);
            runners.add(new TaskRunner(i, 0.5, serverId, rackId));
        }
        scheduler = new GreedyScheduler(runners);
        hydrogelStates = new double[serverTemps.size()];
    }

    public void runEpoch() {
        System.out.println("\n--- New Epoch ---");

        for (int i = 0; i < tasks.size(); i++) {
            scheduler.assignTask(tasks.remove(0));
        }

        for (TaskRunner runner : runners) {
            runner.evaluateSprint();
        }

        //count sprinters per server
        Map<Integer, Integer> sprintersPerServer = new HashMap<>();
        for (TaskRunner runner : runners) {
            if (runner.isSprinting()) {
                sprintersPerServer.merge(runner.getServerId(), 1, Integer::sum);
            }
        }

        //update server temperatures and check for thermal failures
        for (Map.Entry<Integer, Double> entry : serverTemps.entrySet()) {
            int serverId = entry.getKey();
            double temp = entry.getValue();
            double hydrogelState = hydrogelStates[serverId];
            int sprinters = sprintersPerServer.getOrDefault(serverId, 0);

            //updated temperature given current temp and number of sprinters this epoch
            temp = computeNewTemperature(temp, sprinters, hydrogelState);

            //make sure temp is in bounds
            temp = Math.max(0.0, Math.min(1.0, temp));
            serverTemps.put(serverId, temp);

            //updated hydrogel state
            hydrogelState = computeNewHydrogelState(hydrogelState, sprinters, hydrogelState);
            hydrogelStates[serverId] = hydrogelState;

            // Check for thermal failure
            if (temp >= 1.0) {
                // All runners on this server enter cooling recovery
                for (TaskRunner runner : runners) {
                    if (runner.getServerId() == serverId) {
                        runner.updateEpochsInRecoveryForThermalFailure();
                    }
                }
                System.out.println("Server " + serverId + " overheated! All runners cooling.");
            }
        }
        //get rack sprinter counts to check for power failures
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
                //all runners in this rack enter power recovery
                for (TaskRunner runner : runners) {
                    if (runner.getRackId() == rackId) {
                        runner.updateEpochsInRecoveryForPowerFailure();
                    }
                }
                System.out.println("Rack " + rackId + " exceeded power limit! All runners recovering.");
            }
        }

        //execute epoch and update runner states
        for (TaskRunner runner : runners) {
            runner.executeEpoch();
            runner.updateState();
            System.out.println(runner);
        }
    }

    public static double computeNewTemperature(double currentTemp, int sprinters, double hydrogelState) {
        // Placeholder
        double heating = sprinters * 0.1;
        double cooling = 0.05;
        return Math.max(1.0, currentTemp + heating - cooling);
    }

    public static double computeNewHydrogelState(double currentState, int sprinters, double hydrogelState) {
        // Placeholder
        double saturationIncrease = sprinters * 0.05;
        double recovery = 0.02;
        return Math.max(0.0, Math.min(1.0, currentState + saturationIncrease - recovery));
    }

    public Map<Integer, Double> getServerTemps() {
        return serverTemps;
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


}
