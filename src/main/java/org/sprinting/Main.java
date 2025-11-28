package org.sprinting;

import org.sprinting.model.DataCenter;
import org.sprinting.model.Task;
import java.util.Random;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        //configuration
        int procsPerServer = 2;
        int serversPerRack = 2;
        int numRunners = 8; 
        int numTasks = 20;
        int epochs = 15; 

        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            Random rand = new Random();
            tasks.add(new Task(i, 3));  
        }

        // Initialize DataCenter
        DataCenter dc = new DataCenter(procsPerServer, serversPerRack, numRunners, tasks);

        // Run simulation for multiple epochs
        for (int epoch = 1; epoch <= epochs; epoch++) {
            System.out.println("\n=== Epoch " + epoch + " ===");
            dc.runEpoch();
            printServerTemps(dc);
        }
    }

    // Helper function to print server temperatures
    private static void printServerTemps(DataCenter dc) {
        System.out.println("Server temperatures:");
        dc.getServerTemps().forEach((serverId, temp) ->
                System.out.printf("  Server %d: %.2f\n", serverId, temp)
        );
    }
}
