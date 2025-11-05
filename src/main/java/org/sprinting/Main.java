package main.java.org.sprinting;

public class Main {
    public static void main(String[] args) {
        Coordinator coordinator = new Coordinator(3); // 3 multiprocessors

        // Stream of tasks
        coordinator.submitTask(new Task("A", 3));
        coordinator.submitTask(new Task("B", 5));
        coordinator.submitTask(new Task("C", 2));
        coordinator.submitTask(new Task("D", 4));
        coordinator.submitTask(new Task("E", 1));

        // Assign tasks greedily
        coordinator.schedule();

        coordinator.printStatus();

        // Execute tasks across all runners
        coordinator.executeAll();
    }
}

