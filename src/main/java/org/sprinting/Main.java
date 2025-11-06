package main.java.org.sprinting;

import main.java.org.sprinting.model.*;
import main.java.org.sprinting.coordinator.*;

import java.util.ArrayList;
import java.util.List;


public class Main {
    public static void main(String[] args) {
        List<TaskRunner> runners = new ArrayList<>();
        runners.add(new TaskRunner("R1", 0.5));
        runners.add(new TaskRunner("R2", 0.5));
        runners.add(new TaskRunner("R3", 0.5));

        GreedyScheduler scheduler = new GreedyScheduler(runners);

        // Stream of tasks
        for (int i = 0; i < 10; i++) {
            scheduler.assignTask(new Task("T" + i, (int) (Math.random() * 5 + 1)));
        }

        EpochSimulator simulator = new EpochSimulator(runners);

        // Simulate 5 epochs
        for (int epoch = 0; epoch < 5; epoch++) {
            simulator.runEpoch();
        }
    }
}



