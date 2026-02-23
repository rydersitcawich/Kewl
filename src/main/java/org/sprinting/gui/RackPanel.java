package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import org.sprinting.model.DataCenter;
import org.sprinting.model.TaskRunner;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

/**
 * Visual representation of a server rack containing multiple servers
 */
public class RackPanel extends VBox {
    
    private int rackId;
    private List<TaskRunner> runners;
    private int procsPerServer;
    private int serversPerRack;
    private DataCenter dataCenter;
    private List<ServerPanel> serverPanels;
    private Label powerLabel;
    private ProgressBar powerBar;
    
    private static final int MAX_RACK_SPRINTS = 6;
    
    public RackPanel(int rackId, List<TaskRunner> runners, int procsPerServer, int serversPerRack, DataCenter dataCenter) {
        this.rackId = rackId;
        this.runners = runners;
        this.procsPerServer = procsPerServer;
        this.serversPerRack = serversPerRack;
        this.dataCenter = dataCenter;
        this.serverPanels = new ArrayList<>();
        
        setSpacing(10);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-border-color: #3498db; -fx-border-width: 3; -fx-background-color: #2c3e50; -fx-background-radius: 10; -fx-border-radius: 10;");
        setPrefWidth(400);
        
        buildPanel();
    }
    
    private void buildPanel() {
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        
        Label rackLabel = new Label("RACK " + rackId);
        rackLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
        
        header.getChildren().add(rackLabel);
        getChildren().add(header);
        
        // Power consumption indicator
        VBox powerBox = new VBox(5);
        powerBox.setAlignment(Pos.CENTER);
        
        powerLabel = new Label("Power: 0 / " + MAX_RACK_SPRINTS + " sprinters");
        powerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        powerBar = new ProgressBar(0);
        powerBar.setPrefWidth(200);
        powerBar.setStyle("-fx-accent: #2ecc71;");
        
        powerBox.getChildren().addAll(powerLabel, powerBar);
        getChildren().add(powerBox);
        
        // Group runners by server
        Map<Integer, List<TaskRunner>> serverMap = new TreeMap<>();
        for (TaskRunner runner : runners) {
            serverMap.computeIfAbsent(runner.getServerId(), k -> new ArrayList<>()).add(runner);
        }
        
        // Create server panels in a grid
        GridPane serversGrid = new GridPane();
        serversGrid.setHgap(10);
        serversGrid.setVgap(10);
        serversGrid.setAlignment(Pos.CENTER);
        
        int row = 0, col = 0;
        int cols = 2; // 2 servers per row
        
        for (Map.Entry<Integer, List<TaskRunner>> entry : serverMap.entrySet()) {
            int serverId = entry.getKey();
            List<TaskRunner> serverRunners = entry.getValue();
            
            ServerPanel serverPanel = new ServerPanel(serverId, serverRunners, dataCenter);
            serverPanels.add(serverPanel);
            
            serversGrid.add(serverPanel, col, row);
            
            col++;
            if (col >= cols) {
                col = 0;
                row++;
            }
        }
        
        getChildren().add(serversGrid);
    }
    
    public void update() {
        // Update power consumption
        int sprintersCount = 0;
        for (TaskRunner runner : runners) {
            if (runner.isSprinting()) {
                sprintersCount++;
            }
        }
        
        powerLabel.setText("Power: " + sprintersCount + " / " + MAX_RACK_SPRINTS + " sprinters");
        double powerRatio = (double) sprintersCount / MAX_RACK_SPRINTS;
        powerBar.setProgress(powerRatio);
        
        // Change color based on power level
        if (powerRatio > 1.0) {
            powerBar.setStyle("-fx-accent: #e74c3c;"); // Red - over limit
            setStyle("-fx-border-color: #e74c3c; -fx-border-width: 3; -fx-background-color: #2c3e50; -fx-background-radius: 10; -fx-border-radius: 10;");
        } else if (powerRatio > 0.8) {
            powerBar.setStyle("-fx-accent: #f39c12;"); // Orange - warning
            setStyle("-fx-border-color: #f39c12; -fx-border-width: 3; -fx-background-color: #2c3e50; -fx-background-radius: 10; -fx-border-radius: 10;");
        } else {
            powerBar.setStyle("-fx-accent: #2ecc71;"); // Green - normal
            setStyle("-fx-border-color: #3498db; -fx-border-width: 3; -fx-background-color: #2c3e50; -fx-background-radius: 10; -fx-border-radius: 10;");
        }
        
        // Update server panels
        for (ServerPanel serverPanel : serverPanels) {
            serverPanel.update();
        }
    }
}