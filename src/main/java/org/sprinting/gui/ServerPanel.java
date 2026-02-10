package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
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
        
        // Task runners (processors) - now with individual chip monitoring
        HBox runnersBox = new HBox(5);
        runnersBox.setAlignment(Pos.CENTER);
        
        for (TaskRunner runner : runners) {
            TaskRunnerPanel runnerPanel = new TaskRunnerPanel(runner, dataCenter);
            runnerPanels.add(runnerPanel);
            runnersBox.getChildren().add(runnerPanel);
        }
        
        getChildren().add(runnersBox);
    }
    
    public void update() {
        // Update runner panels (which now handle their own temp and hydrogel)
        for (TaskRunnerPanel runnerPanel : runnerPanels) {
            runnerPanel.update();
        }
    }
}