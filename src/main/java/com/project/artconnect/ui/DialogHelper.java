package com.project.artconnect.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utility class providing simple helpers for CRUD dialogs across controllers.
 */
public final class DialogHelper {
    private DialogHelper() {
    }

    /**
     * Shows an error alert with the given title and message.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows an information alert.
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Shows a confirmation alert and returns true if the user confirms.
     */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Parses a non-empty trimmed string. Returns null if blank.
     */
    public static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Parses an integer safely. Returns null on empty or invalid input.
     */
    public static Integer parseIntOrNull(String value) {
        String trimmed = emptyToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a double safely. Returns 0.0 on empty or invalid input.
     */
    public static double parseDoubleOrZero(String value) {
        String trimmed = emptyToNull(value);
        if (trimmed == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
