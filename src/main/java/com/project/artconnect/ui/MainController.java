package com.project.artconnect.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.application.Platform;

public class MainController {
    @FXML
    private TabPane mainTabPane;

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}
