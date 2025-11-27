package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.sprinting.model.DataCenter;
import org.sprinting.model.TaskRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual representation of a server containing multiple task runners (processors)
 */
public class ServerPanel extends VBox {
    
    private int serverId;
    private List<TaskRunner> runners;
    private DataCenter dataCenter;
    private List<TaskRunnerPanel> runnerPanels;
    private Label tempLabel;
    private ProgressBar tempBar;
    private ProgressBar hydrogelBar;
    private Label hydrogelLabel;
    
    public ServerPanel(int serverId, List<TaskRunner> runners, DataCenter dataCenter) {
        this.serverId = serverId;
        this.runners = runners;
        this.dataCenter = dataCenter;
        this.runnerPanels = new ArrayList<>();
        
        setSpacing(8);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-border-color: #95a5a6; -fx-border-width: 2; -fx-background-color: #34495e; -fx-background-radius: 5; -fx-border-radius: 5;");
        setPrefWidth(180);
        
        buildPanel();
    }
    
    private void buildPanel() {
        // Header
        Label serverLabel = new Label("Server " + serverId);
        serverLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ecf0f1;");
        getChildren().add(serverLabel);
        
        // Temperature indicator
        VBox tempBox = new VBox(3);
        tempBox.setAlignment(Pos.CENTER);
        
        tempLabel = new Label("Temp: 0.00");
        tempLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        
        tempBar = new ProgressBar(0);
        tempBar.setPrefWidth(150);
        tempBar.setStyle("-fx-accent: #3498db;");
        
        tempBox.getChildren().addAll(tempLabel, tempBar);
        getChildren().add(tempBox);
        
        // Hydrogel state indicator
        VBox hydrogelBox = new VBox(3);
        hydrogelBox.setAlignment(Pos.CENTER);
        
        hydrogelLabel = new Label("Hydrogel: 0.00");
        hydrogelLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");
        
        hydrogelBar = new ProgressBar(0);
        hydrogelBar.setPrefWidth(150);
        hydrogelBar.setStyle("-fx-accent: #1abc9c;");
        
        hydrogelBox.getChildren().addAll(hydrogelLabel, hydrogelBar);
        getChildren().add(hydrogelBox);
        
        // Task runners (processors)
        HBox runnersBox = new HBox(5);
        runnersBox.setAlignment(Pos.CENTER);
        
        for (TaskRunner runner : runners) {
            TaskRunnerPanel runnerPanel = new TaskRunnerPanel(runner);
            runnerPanels.add(runnerPanel);
            runnersBox.getChildren().add(runnerPanel);
        }
        
        getChildren().add(runnersBox);
    }
    
    public void update() {
        // Update temperature
        double temp = dataCenter.getServerTemps().getOrDefault(serverId, 0.0);
        tempLabel.setText(String.format("Temp: %.2f", temp));
        tempBar.setProgress(temp);
        
        // Color temperature bar based on heat level
        if (temp >= 1.0) {
            tempBar.setStyle("-fx-accent: #e74c3c;"); // Red - overheated
        } else if (temp >= 0.7) {
            tempBar.setStyle("-fx-accent: #f39c12;"); // Orange - hot
        } else if (temp >= 0.4) {
            tempBar.setStyle("-fx-accent: #f1c40f;"); // Yellow - warm
        } else {
            tempBar.setStyle("-fx-accent: #3498db;"); // Blue - cool
        }
        
        // Update hydrogel state
        double[] hydrogelStates = dataCenter.getHydrogelStates();
        double hydrogelState = (serverId < hydrogelStates.length) ? hydrogelStates[serverId] : 0.0;
        hydrogelLabel.setText(String.format("Hydrogel: %.2f", hydrogelState));
        hydrogelBar.setProgress(hydrogelState);
        
        // Color hydrogel bar
        if (hydrogelState >= 0.8) {
            hydrogelBar.setStyle("-fx-accent: #e74c3c;"); // Red - saturated
        } else if (hydrogelState >= 0.5) {
            hydrogelBar.setStyle("-fx-accent: #f39c12;"); // Orange - medium
        } else {
            hydrogelBar.setStyle("-fx-accent: #1abc9c;"); // Teal - good
        }
        
        // Update runner panels
        for (TaskRunnerPanel runnerPanel : runnerPanels) {
            runnerPanel.update();
        }
    }
}