package com.project.artconnect.ui;

import com.project.artconnect.util.ServiceProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.application.Platform;

public class MainController {
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Label modeLabel;

    @FXML
    public void initialize() {
        String mode = ServiceProvider.isUsingJdbc() ? "Database" : "In-Memory";
        modeLabel.setText("ArtConnect Pro v1.0 | Mode: " + mode);
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}
