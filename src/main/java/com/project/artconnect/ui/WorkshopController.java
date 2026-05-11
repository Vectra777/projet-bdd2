package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class WorkshopController {
    @FXML
    private TableView<Workshop> workshopTable;
    @FXML
    private TableColumn<Workshop, String> titleColumn;
    @FXML
    private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML
    private TableColumn<Workshop, String> instructorColumn;
    @FXML
    private TableColumn<Workshop, Double> priceColumn;
    @FXML
    private TableColumn<Workshop, String> levelColumn;

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));

        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null ? cellData.getValue().getInstructor().getName()
                        : "Unknown"));

        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<Workshop> result = showWorkshopDialog(null);
        if (result.isPresent()) {
            try {
                workshopService.createWorkshop(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Workshop", "Unable to add workshop: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Workshop", "Please select a workshop to edit.");
            return;
        }
        Optional<Workshop> result = showWorkshopDialog(selected);
        if (result.isPresent()) {
            try {
                workshopService.updateWorkshop(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Workshop", "Unable to update workshop: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Workshop selected = workshopTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Workshop", "Please select a workshop to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Workshop", "Delete workshop '" + selected.getTitle() + "'?")) {
            try {
                workshopService.deleteWorkshop(selected.getTitle());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Workshop", "Unable to delete workshop: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
    }

    private Optional<Workshop> showWorkshopDialog(Workshop existing) {
        Dialog<Workshop> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Workshop" : "Edit Workshop");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        ComboBox<Artist> instructorBox = new ComboBox<>(
                FXCollections.observableArrayList(artistService.getAllArtists()));
        DatePicker datePicker = new DatePicker();
        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");
        TextField durationField = new TextField();
        TextField maxParticipantsField = new TextField();
        TextField priceField = new TextField();
        TextField locationField = new TextField();
        ComboBox<String> levelBox = new ComboBox<>(
                FXCollections.observableArrayList("Beginner", "Intermediate", "Advanced"));
        TextField descriptionField = new TextField();

        if (existing != null) {
            titleField.setText(existing.getTitle() != null ? existing.getTitle() : "");
            titleField.setEditable(false);
            instructorBox.setValue(existing.getInstructor());
            if (existing.getDate() != null) {
                datePicker.setValue(existing.getDate().toLocalDate());
                timeField.setText(existing.getDate().toLocalTime().toString());
            }
            durationField.setText(String.valueOf(existing.getDurationMinutes()));
            maxParticipantsField.setText(String.valueOf(existing.getMaxParticipants()));
            priceField.setText(String.valueOf(existing.getPrice()));
            locationField.setText(existing.getLocation() != null ? existing.getLocation() : "");
            levelBox.setValue(existing.getLevel());
            descriptionField.setText(existing.getDescription() != null ? existing.getDescription() : "");
        } else {
            timeField.setText("10:00");
            durationField.setText("180");
            maxParticipantsField.setText("10");
            levelBox.setValue("Beginner");
        }

        grid.add(new Label("Title *:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Instructor *:"), 0, 1);
        grid.add(instructorBox, 1, 1);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Time (HH:mm):"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label("Duration (min):"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(new Label("Max Participants:"), 0, 5);
        grid.add(maxParticipantsField, 1, 5);
        grid.add(new Label("Price:"), 0, 6);
        grid.add(priceField, 1, 6);
        grid.add(new Label("Location:"), 0, 7);
        grid.add(locationField, 1, 7);
        grid.add(new Label("Level:"), 0, 8);
        grid.add(levelBox, 1, 8);
        grid.add(new Label("Description:"), 0, 9);
        grid.add(descriptionField, 1, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String title = DialogHelper.emptyToNull(titleField.getText());
            Artist instructor = instructorBox.getValue();
            if (title == null) {
                DialogHelper.showError("Validation", "Title is required.");
                return null;
            }
            if (instructor == null) {
                DialogHelper.showError("Validation", "Instructor is required.");
                return null;
            }
            Workshop workshop = existing != null ? existing : new Workshop();
            workshop.setTitle(title);
            workshop.setInstructor(instructor);
            LocalDate date = datePicker.getValue();
            LocalTime time;
            try {
                time = LocalTime.parse(timeField.getText() != null ? timeField.getText().trim() : "10:00");
            } catch (Exception e) {
                time = LocalTime.of(10, 0);
            }
            if (date != null) {
                workshop.setDate(LocalDateTime.of(date, time));
            }
            Integer duration = DialogHelper.parseIntOrNull(durationField.getText());
            workshop.setDurationMinutes(duration != null ? duration : 180);
            Integer max = DialogHelper.parseIntOrNull(maxParticipantsField.getText());
            workshop.setMaxParticipants(max != null ? max : 10);
            workshop.setPrice(DialogHelper.parseDoubleOrZero(priceField.getText()));
            workshop.setLocation(DialogHelper.emptyToNull(locationField.getText()));
            workshop.setLevel(levelBox.getValue());
            workshop.setDescription(DialogHelper.emptyToNull(descriptionField.getText()));
            return workshop;
        });

        return dialog.showAndWait();
    }
}
