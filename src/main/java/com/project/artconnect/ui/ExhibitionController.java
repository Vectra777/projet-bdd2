package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Optional;

public class ExhibitionController {
    @FXML
    private TableView<Exhibition> exhibitionTable;
    @FXML
    private TableColumn<Exhibition, String> titleColumn;
    @FXML
    private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML
    private TableColumn<Exhibition, String> themeColumn;
    @FXML
    private TableColumn<Exhibition, String> galleryColumn;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        themeColumn.setCellValueFactory(new PropertyValueFactory<>("theme"));

        galleryColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGallery() != null ? cellData.getValue().getGallery().getName() : "Unknown"));

        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<Exhibition> result = showExhibitionDialog(null);
        if (result.isPresent()) {
            try {
                exhibitionService.createExhibition(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Exhibition", "Unable to add exhibition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Exhibition", "Please select an exhibition to edit.");
            return;
        }
        Optional<Exhibition> result = showExhibitionDialog(selected);
        if (result.isPresent()) {
            try {
                exhibitionService.updateExhibition(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Exhibition", "Unable to update exhibition: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Exhibition selected = exhibitionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Exhibition", "Please select an exhibition to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Exhibition", "Delete exhibition '" + selected.getTitle() + "'?")) {
            try {
                exhibitionService.deleteExhibition(selected.getTitle());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Exhibition", "Unable to delete exhibition: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        exhibitionTable.setItems(FXCollections.observableArrayList(exhibitionService.getAllExhibitions()));
    }

    private Optional<Exhibition> showExhibitionDialog(Exhibition existing) {
        Dialog<Exhibition> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Exhibition" : "Edit Exhibition");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        ComboBox<Gallery> galleryBox = new ComboBox<>(
                FXCollections.observableArrayList(galleryService.getAllGalleries()));
        DatePicker startPicker = new DatePicker();
        DatePicker endPicker = new DatePicker();
        TextField themeField = new TextField();
        TextField curatorField = new TextField();
        TextField descriptionField = new TextField();

        if (existing != null) {
            titleField.setText(existing.getTitle() != null ? existing.getTitle() : "");
            titleField.setEditable(false);
            galleryBox.setValue(existing.getGallery());
            startPicker.setValue(existing.getStartDate());
            endPicker.setValue(existing.getEndDate());
            themeField.setText(existing.getTheme() != null ? existing.getTheme() : "");
            curatorField.setText(existing.getCuratorName() != null ? existing.getCuratorName() : "");
            descriptionField.setText(existing.getDescription() != null ? existing.getDescription() : "");
        }

        grid.add(new Label("Title *:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Gallery *:"), 0, 1);
        grid.add(galleryBox, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startPicker, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endPicker, 1, 3);
        grid.add(new Label("Theme:"), 0, 4);
        grid.add(themeField, 1, 4);
        grid.add(new Label("Curator:"), 0, 5);
        grid.add(curatorField, 1, 5);
        grid.add(new Label("Description:"), 0, 6);
        grid.add(descriptionField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String title = DialogHelper.emptyToNull(titleField.getText());
            Gallery gallery = galleryBox.getValue();
            if (title == null) {
                DialogHelper.showError("Validation", "Title is required.");
                return null;
            }
            if (gallery == null) {
                DialogHelper.showError("Validation", "Gallery is required.");
                return null;
            }
            Exhibition exhibition = existing != null ? existing : new Exhibition();
            exhibition.setTitle(title);
            exhibition.setGallery(gallery);
            exhibition.setStartDate(startPicker.getValue());
            exhibition.setEndDate(endPicker.getValue());
            exhibition.setTheme(DialogHelper.emptyToNull(themeField.getText()));
            exhibition.setCuratorName(DialogHelper.emptyToNull(curatorField.getText()));
            exhibition.setDescription(DialogHelper.emptyToNull(descriptionField.getText()));
            return exhibition;
        });

        return dialog.showAndWait();
    }
}
