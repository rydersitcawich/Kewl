package main.java.org.sprinting.model;

public enum RunnerState {
    ACTIVE,     // Can sprint
    COOLING,    // Cooling after a sprint
    RECOVERY    // Rack recovery (can’t sprint)
}
