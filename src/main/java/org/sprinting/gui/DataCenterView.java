package org.sprinting.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.control.Label;
import org.sprinting.model.DataCenter;
import org.sprinting.model.TaskRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Main visualization of the entire data center showing racks, servers, and task runners
 */
public class DataCenterView extends VBox {
    
    private DataCenter dataCenter;
    private int procsPerServer;
    private int serversPerRack;
    private List<RackPanel> rackPanels;
    
    public DataCenterView(DataCenter dataCenter, int procsPerServer, int serversPerRack) {
        this.dataCenter = dataCenter;
        this.procsPerServer = procsPerServer;
        this.serversPerRack = serversPerRack;
        this.rackPanels = new ArrayList<>();
        
        setSpacing(15);
        setPadding(new Insets(15));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #1a1a1a;");
        
        buildView();
    }
    
    private void buildView() {
        getChildren().clear();
        rackPanels.clear();
        
        // Title
        Label title = new Label("Data Center Layout");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        getChildren().add(title);
        
        // Group runners by rack
        int numRacks = (int) Math.ceil((double) dataCenter.getRunners().size() / (procsPerServer * serversPerRack));
        
        // Create rack panels in a grid
        FlowPane racksContainer = new FlowPane();
        racksContainer.setHgap(20);
        racksContainer.setVgap(20);
        racksContainer.setAlignment(Pos.CENTER);
        
        for (int rackId = 0; rackId < numRacks; rackId++) {
            List<TaskRunner> rackRunners = getRunnersForRack(rackId);
            RackPanel rackPanel = new RackPanel(rackId, rackRunners, procsPerServer, serversPerRack, dataCenter);
            rackPanels.add(rackPanel);
            racksContainer.getChildren().add(rackPanel);
        }
        
        getChildren().add(racksContainer);
    }
    
    private List<TaskRunner> getRunnersForRack(int rackId) {
        List<TaskRunner> rackRunners = new ArrayList<>();
        for (TaskRunner runner : dataCenter.getRunners()) {
            if (runner.getRackId() == rackId) {
                rackRunners.add(runner);
            }
        }
        return rackRunners;
    }
    
    public void update() {
        for (RackPanel rackPanel : rackPanels) {
            rackPanel.update();
        }
    }
    
    public void setDataCenter(DataCenter dataCenter) {
        this.dataCenter = dataCenter;
        buildView();
    }
}