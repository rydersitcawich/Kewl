package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.sprinting.model.TaskRunner;

/**
 * Visual representation of a single task runner (processor/core)
 */
public class TaskRunnerPanel extends VBox {
    
    private TaskRunner runner;
    private Label idLabel;
    private Label workLabel;
    private Pane statusIndicator;
    private Tooltip tooltip;
    
    public TaskRunnerPanel(TaskRunner runner) {
        this.runner = runner;
        
        setSpacing(3);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefSize(70, 90);
        setStyle("-fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
        
        buildPanel();
        update();
    }
    
    private void buildPanel() {
        // Runner ID
        idLabel = new Label("R" + runner.getId());
        idLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Status indicator (colored circle)
        statusIndicator = new Pane();
        statusIndicator.setPrefSize(40, 40);
        statusIndicator.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: white; -fx-border-width: 2;");
        
        // Work count
        workLabel = new Label("W: 0");
        workLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: white;");
        
        // Tooltip for detailed info
        tooltip = new Tooltip();
        Tooltip.install(this, tooltip);
        
        getChildren().addAll(idLabel, statusIndicator, workLabel);
    }
    
    public void update() {
        // Update work count
        int totalWork = runner.getTotalWork();
        workLabel.setText("W: " + totalWork);
        
        // Determine state and color
        String state;
        String color;
        String statusText;
        
        if (!runner.canSprint()) {
            // Recovering
            state = "RECOVERING";
            color = "#e74c3c"; // Red
            statusText = "🔴";
        } else if (runner.isSprinting()) {
            // Sprinting
            state = "SPRINTING";
            color = "#f39c12"; // Orange
            statusText = "⚡";
        } else if (totalWork > 0) {
            // Working normally
            state = "WORKING";
            color = "#f1c40f"; // Yellow
            statusText = "⚙️";
        } else {
            // Idle
            state = "IDLE";
            color = "#2ecc71"; // Green
            statusText = "✓";
        }
        
        // Update background color
        setStyle(String.format(
            "-fx-border-color: #7f8c8d; -fx-border-width: 1; " +
            "-fx-background-color: %s; " +
            "-fx-background-radius: 3; -fx-border-radius: 3;",
            color
        ));
        
        // Update status indicator
        statusIndicator.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-background-radius: 20; -fx-border-radius: 20; " +
            "-fx-border-color: white; -fx-border-width: 2;",
            darkenColor(color)
        ));
        
        // Update tooltip
        tooltip.setText(String.format(
            "Runner %d\n" +
            "Server: %d | Rack: %d\n" +
            "State: %s\n" +
            "Total Work: %d\n" +
            "Can Sprint: %s",
            runner.getId(),
            runner.getServerId(),
            runner.getRackId(),
            state,
            totalWork,
            runner.canSprint() ? "Yes" : "No"
        ));
    }
    
    private String darkenColor(String hexColor) {
        // Simple darkening by reducing each RGB component
        try {
            Color color = Color.web(hexColor);
            double factor = 0.7;
            Color darker = new Color(
                color.getRed() * factor,
                color.getGreen() * factor,
                color.getBlue() * factor,
                color.getOpacity()
            );
            return String.format("#%02X%02X%02X",
                (int)(darker.getRed() * 255),
                (int)(darker.getGreen() * 255),
                (int)(darker.getBlue() * 255)
            );
        } catch (Exception e) {
            return hexColor;
        }
    }
}