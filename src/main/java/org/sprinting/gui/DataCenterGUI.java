package org.sprinting.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.sprinting.model.DataCenter;
import org.sprinting.model.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Main GUI application for the Data Center Simulator
 */
public class DataCenterGUI extends Application {
    
    private DataCenter dataCenter;
    private DataCenterView dataCenterView;
    private Label epochLabel;
    private Label statsLabel;
    private TextArea logArea;
    private Button playPauseButton;
    private Button stepButton;
    private Slider speedSlider;
    
    private SimulationThread simulationThread;
    private boolean isRunning = false;
    private int currentEpoch = 0;
    private int simulationSpeed = 1000; // milliseconds per epoch
    
    // Configuration
    private static final int PROCS_PER_SERVER = 2;
    private static final int SERVERS_PER_RACK = 2;
    private static final int NUM_RUNNERS = 8;
    private static final int INITIAL_TASKS = 20;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Data Center Computational Sprinting Simulator");
        
        // Initialize the data center
        initializeDataCenter();
        
        // Create main layout
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Top: Control panel
        root.setTop(createControlPanel());
        
        // Center: Data center visualization
        ScrollPane scrollPane = new ScrollPane();
        dataCenterView = new DataCenterView(dataCenter, PROCS_PER_SERVER, SERVERS_PER_RACK);
        scrollPane.setContent(dataCenterView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        root.setCenter(scrollPane);
        
        // Right: Metrics and log panel
        root.setRight(createMetricsPanel());
        
        // Create scene
        Scene scene = new Scene(root, 1400, 800);
        // Optional: Uncomment if you have the CSS file
        // scene.getStylesheets().add(getClass().getResource("/datacenter-styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initial update
        updateVisualization();
        
        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            stopSimulation();
            Platform.exit();
        });
    }
    
    private void initializeDataCenter() {
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < INITIAL_TASKS; i++) {
            tasks.add(new Task(i, 3 + (int)(Math.random() * 5)));
        }
        dataCenter = new DataCenter(PROCS_PER_SERVER, SERVERS_PER_RACK, NUM_RUNNERS, tasks);
    }
    
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #2c3e50; -fx-background-radius: 5;");
        
        // Title
        Label titleLabel = new Label("Computational Sprinting Data Center");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Epoch display
        epochLabel = new Label("Epoch: 0");
        epochLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        playPauseButton = new Button("▶ Play");
        playPauseButton.setOnAction(e -> toggleSimulation());
        
        stepButton = new Button("⏭ Step");
        stepButton.setOnAction(e -> stepSimulation());
        
        Button resetButton = new Button("🔄 Reset");
        resetButton.setOnAction(e -> resetSimulation());
        
        Button addTasksButton = new Button("➕ Add Tasks");
        addTasksButton.setOnAction(e -> addRandomTasks());
        
        buttonBox.getChildren().addAll(playPauseButton, stepButton, resetButton, addTasksButton);
        
        // Speed control
        HBox speedBox = new HBox(10);
        speedBox.setAlignment(Pos.CENTER);
        Label speedLabel = new Label("Speed:");
        speedLabel.setStyle("-fx-text-fill: white;");
        
        speedSlider = new Slider(100, 2000, 1000);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(500);
        speedSlider.setPrefWidth(200);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = newVal.intValue();
        });
        
        Label speedValueLabel = new Label("1.0x");
        speedValueLabel.setStyle("-fx-text-fill: white;");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double speed = 1000.0 / newVal.doubleValue();
            speedValueLabel.setText(String.format("%.1fx", speed));
        });
        
        speedBox.getChildren().addAll(speedLabel, speedSlider, speedValueLabel);
        
        controlPanel.getChildren().addAll(titleLabel, epochLabel, buttonBox, speedBox);
        return controlPanel;
    }
    
    private VBox createMetricsPanel() {
        VBox metricsPanel = new VBox(10);
        metricsPanel.setPadding(new Insets(10));
        metricsPanel.setPrefWidth(300);
        metricsPanel.setStyle("-fx-background-color: #34495e; -fx-background-radius: 5;");
        
        Label metricsTitle = new Label("System Metrics");
        metricsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        statsLabel = new Label();
        statsLabel.setStyle("-fx-text-fill: white; -fx-font-family: monospace;");
        updateStats();
        
        Label logTitle = new Label("Event Log");
        logTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(20);
        logArea.setStyle("-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; -fx-font-family: monospace;");
        
        metricsPanel.getChildren().addAll(metricsTitle, statsLabel, new Separator(), logTitle, logArea);
        return metricsPanel;
    }
    
    private void toggleSimulation() {
        if (isRunning) {
            stopSimulation();
        } else {
            startSimulation();
        }
    }
    
    private void startSimulation() {
        isRunning = true;
        playPauseButton.setText("⏸ Pause");
        stepButton.setDisable(true);
        
        simulationThread = new SimulationThread();
        simulationThread.start();
    }
    
    private void stopSimulation() {
        isRunning = false;
        playPauseButton.setText("▶ Play");
        stepButton.setDisable(false);
        
        if (simulationThread != null) {
            simulationThread.interrupt();
        }
    }
    
    private void stepSimulation() {
        currentEpoch++;
        runEpochWithLogging();
        updateVisualization();
    }
    
    private void resetSimulation() {
        stopSimulation();
        currentEpoch = 0;
        initializeDataCenter();
        dataCenterView.setDataCenter(dataCenter);
        logArea.clear();
        updateVisualization();
        log("System reset");
    }
    
    private void addRandomTasks() {
        int numTasks = 5 + (int)(Math.random() * 10);
        List<Task> newTasks = new ArrayList<>();
        for (int i = 0; i < numTasks; i++) {
            newTasks.add(new Task(1000 + currentEpoch * 100 + i, 3 + (int)(Math.random() * 5)));
        }
        dataCenter.addTasks(newTasks);
        log("Added " + numTasks + " new tasks to the queue");
        updateVisualization();
    }
    
    private void runEpochWithLogging() {
        log("=== Epoch " + currentEpoch + " ===");
        dataCenter.runEpoch();
    }
    
    private void updateVisualization() {
        Platform.runLater(() -> {
            epochLabel.setText("Epoch: " + currentEpoch);
            dataCenterView.update();
            updateStats();
        });
    }
    
    private void updateStats() {
        int totalRunners = NUM_RUNNERS;
        long sprinting = dataCenter.getRunners().stream().filter(r -> r.isSprinting()).count();
        long recovering = dataCenter.getRunners().stream().filter(r -> !r.canSprint()).count();
        long idle = dataCenter.getRunners().stream().filter(r -> r.getTotalWork() == 0).count();
        int pendingTasks = dataCenter.getTasks().size();
        
        double avgTemp = dataCenter.getServerTemps().values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        String stats = String.format(
            "Runners: %d\n" +
            "  Sprinting: %d\n" +
            "  Recovering: %d\n" +
            "  Idle: %d\n" +
            "\nPending Tasks: %d\n" +
            "\nAvg Temperature: %.2f",
            totalRunners, sprinting, recovering, idle, pendingTasks, avgTemp
        );
        
        statsLabel.setText(stats);
    }
    
    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }
    
    private class SimulationThread extends Thread {
        @Override
        public void run() {
            while (isRunning && !isInterrupted()) {
                try {
                    Thread.sleep(simulationSpeed);
                    Platform.runLater(() -> {
                        currentEpoch++;
                        runEpochWithLogging();
                        updateVisualization();
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}