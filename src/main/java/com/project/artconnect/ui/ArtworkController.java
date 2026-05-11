package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class ArtworkController {
    @FXML
    private TableView<Artwork> artworkTable;
    @FXML
    private TableColumn<Artwork, String> titleColumn;
    @FXML
    private TableColumn<Artwork, String> typeColumn;
    @FXML
    private TableColumn<Artwork, Double> priceColumn;
    @FXML
    private TableColumn<Artwork, String> statusColumn;
    @FXML
    private TableColumn<Artwork, String> artistColumn;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null ? cellData.getValue().getArtist().getName() : "Unknown"));

        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<Artwork> result = showArtworkDialog(null);
        if (result.isPresent()) {
            try {
                artworkService.createArtwork(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Artwork", "Unable to add artwork: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Artwork", "Please select an artwork to edit.");
            return;
        }
        Optional<Artwork> result = showArtworkDialog(selected);
        if (result.isPresent()) {
            try {
                artworkService.updateArtwork(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Artwork", "Unable to update artwork: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Artwork selected = artworkTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Artwork", "Please select an artwork to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Artwork", "Delete artwork '" + selected.getTitle() + "'?")) {
            try {
                artworkService.deleteArtwork(selected.getTitle());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Artwork", "Unable to delete artwork: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
    }

    private Optional<Artwork> showArtworkDialog(Artwork existing) {
        Dialog<Artwork> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Artwork" : "Edit Artwork");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField titleField = new TextField();
        ComboBox<Artist> artistBox = new ComboBox<>(FXCollections.observableArrayList(artistService.getAllArtists()));
        TextField typeField = new TextField();
        TextField priceField = new TextField();
        ComboBox<Artwork.Status> statusBox = new ComboBox<>(FXCollections.observableArrayList(Artwork.Status.values()));
        TextField yearField = new TextField();
        TextField mediumField = new TextField();
        TextField dimensionsField = new TextField();

        if (existing != null) {
            titleField.setText(existing.getTitle() != null ? existing.getTitle() : "");
            titleField.setEditable(false);
            artistBox.setValue(existing.getArtist());
            typeField.setText(existing.getType() != null ? existing.getType() : "");
            priceField.setText(String.valueOf(existing.getPrice()));
            statusBox.setValue(existing.getStatus());
            yearField.setText(existing.getCreationYear() != null ? String.valueOf(existing.getCreationYear()) : "");
            mediumField.setText(existing.getMedium() != null ? existing.getMedium() : "");
            dimensionsField.setText(existing.getDimensions() != null ? existing.getDimensions() : "");
        } else {
            statusBox.setValue(Artwork.Status.FOR_SALE);
        }

        grid.add(new Label("Title *:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Artist *:"), 0, 1);
        grid.add(artistBox, 1, 1);
        grid.add(new Label("Type:"), 0, 2);
        grid.add(typeField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusBox, 1, 4);
        grid.add(new Label("Creation Year:"), 0, 5);
        grid.add(yearField, 1, 5);
        grid.add(new Label("Medium:"), 0, 6);
        grid.add(mediumField, 1, 6);
        grid.add(new Label("Dimensions:"), 0, 7);
        grid.add(dimensionsField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String title = DialogHelper.emptyToNull(titleField.getText());
            Artist artist = artistBox.getValue();
            if (title == null) {
                DialogHelper.showError("Validation", "Title is required.");
                return null;
            }
            if (artist == null) {
                DialogHelper.showError("Validation", "Artist is required.");
                return null;
            }
            Artwork artwork = existing != null ? existing : new Artwork();
            artwork.setTitle(title);
            artwork.setArtist(artist);
            artwork.setType(DialogHelper.emptyToNull(typeField.getText()));
            artwork.setPrice(DialogHelper.parseDoubleOrZero(priceField.getText()));
            artwork.setStatus(statusBox.getValue() != null ? statusBox.getValue() : Artwork.Status.FOR_SALE);
            artwork.setCreationYear(DialogHelper.parseIntOrNull(yearField.getText()));
            artwork.setMedium(DialogHelper.emptyToNull(mediumField.getText()));
            artwork.setDimensions(DialogHelper.emptyToNull(dimensionsField.getText()));
            return artwork;
        });

        return dialog.showAndWait();
    }
}
