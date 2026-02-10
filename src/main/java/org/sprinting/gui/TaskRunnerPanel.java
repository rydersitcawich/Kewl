package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.sprinting.model.DataCenter;
import org.sprinting.model.TaskRunner;

/**
 * Visual representation of a single task runner (processor/core) with chip-level monitoring
 */
public class TaskRunnerPanel extends VBox {
    
    private TaskRunner runner;
    private DataCenter dataCenter;
    private Label idLabel;
    private Label workLabel;
    private Pane statusIndicator;
    private ProgressBar tempBar;
    private ProgressBar hydrogelBar;
    private Tooltip tooltip;
    
    public TaskRunnerPanel(TaskRunner runner, DataCenter dataCenter) {
        this.runner = runner;
        this.dataCenter = dataCenter;
        
        setSpacing(3);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefSize(70, 120);
        setStyle("-fx-border-color: #7f8c8d; -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;");
        
        buildPanel();
        update();
    }
    
    private void buildPanel() {
        // Runner ID
        idLabel = new Label("R" + runner.getId());
        idLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Status indicator (colored circle)
        statusIndicator = new Pane();
        statusIndicator.setPrefSize(30, 30);
        statusIndicator.setStyle("-fx-background-radius: 15; -fx-border-radius: 15; -fx-border-color: white; -fx-border-width: 2;");
        
        // Work count
        workLabel = new Label("W: 0");
        workLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white;");
        
        // Temperature bar
        tempBar = new ProgressBar(0);
        tempBar.setPrefWidth(60);
        tempBar.setPrefHeight(6);
        tempBar.setStyle("-fx-accent: #3498db;");
        
        // Hydrogel bar
        hydrogelBar = new ProgressBar(0);
        hydrogelBar.setPrefWidth(60);
        hydrogelBar.setPrefHeight(6);
        hydrogelBar.setStyle("-fx-accent: #1abc9c;");
        
        // Tooltip for detailed info
        tooltip = new Tooltip();
        Tooltip.install(this, tooltip);
        
        getChildren().addAll(idLabel, statusIndicator, workLabel, tempBar, hydrogelBar);
    }
    
    public void update() {
        // Update work count
        int totalWork = runner.getTotalWork();
        workLabel.setText("W: " + totalWork);
        
        // Get chip-level data
        double chipTemp = dataCenter.getChipTemp(runner.getId());
        double hydrogelState = dataCenter.getHydrogelState(runner.getId());
        
        // Determine state and color
        String state;
        String color;
        
        if (!runner.canSprint()) {
            // Recovering
            state = "RECOVERING";
            color = "#e74c3c"; // Red
        } else if (runner.isSprinting()) {
            // Sprinting
            state = "SPRINTING";
            color = "#f39c12"; // Orange
        } else if (totalWork > 0) {
            // Working normally
            state = "WORKING";
            color = "#f1c40f"; // Yellow
        } else {
            // Idle
            state = "IDLE";
            color = "#2ecc71"; // Green
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
            "-fx-background-radius: 15; -fx-border-radius: 15; " +
            "-fx-border-color: white; -fx-border-width: 2;",
            darkenColor(color)
        ));
        
        // Update temperature bar
        tempBar.setProgress(chipTemp);
        if (chipTemp >= 1.0) {
            tempBar.setStyle("-fx-accent: #e74c3c;"); // Red - at limit (throttling)
        } else if (chipTemp >= 0.7) {
            tempBar.setStyle("-fx-accent: #f39c12;"); // Orange - hot
        } else if (chipTemp >= 0.4) {
            tempBar.setStyle("-fx-accent: #f1c40f;"); // Yellow - warm
        } else {
            tempBar.setStyle("-fx-accent: #3498db;"); // Blue - cool
        }
        
        // Update hydrogel bar
        hydrogelBar.setProgress(hydrogelState);
        if (hydrogelState <= 0.2) {
            hydrogelBar.setStyle("-fx-accent: #e74c3c;"); // Red - depleted
        } else if (hydrogelState <= 0.5) {
            hydrogelBar.setStyle("-fx-accent: #f39c12;"); // Orange - low
        } else {
            hydrogelBar.setStyle("-fx-accent: #1abc9c;"); // Teal - good
        }
        
        // Update tooltip
        tooltip.setText(String.format(
            "Runner %d\n" +
            "Server: %d | Rack: %d\n" +
            "State: %s\n" +
            "Total Work: %d\n" +
            "Can Sprint: %s\n" +
            "---\n" +
            "Chip Temp: %.2f\n" +
            "Hydrogel: %.2f",
            runner.getId(),
            runner.getServerId(),
            runner.getRackId(),
            state,
            totalWork,
            runner.canSprint() ? "Yes" : "No",
            chipTemp,
            hydrogelState
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