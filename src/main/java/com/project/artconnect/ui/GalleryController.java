package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class GalleryController {
    @FXML
    private TableView<Gallery> galleryTable;
    @FXML
    private TableColumn<Gallery, String> nameColumn;
    @FXML
    private TableColumn<Gallery, String> addressColumn;
    @FXML
    private TableColumn<Gallery, Double> ratingColumn;
    @FXML
    private TableColumn<Gallery, String> ownerColumn;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        ratingColumn.setCellValueFactory(new PropertyValueFactory<>("rating"));
        ownerColumn.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<Gallery> result = showGalleryDialog(null);
        if (result.isPresent()) {
            try {
                galleryService.createGallery(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Gallery", "Unable to add gallery: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Gallery", "Please select a gallery to edit.");
            return;
        }
        Optional<Gallery> result = showGalleryDialog(selected);
        if (result.isPresent()) {
            try {
                galleryService.updateGallery(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Gallery", "Unable to update gallery: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Gallery selected = galleryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Gallery", "Please select a gallery to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Gallery", "Delete gallery '" + selected.getName() + "'?")) {
            try {
                galleryService.deleteGallery(selected.getName());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Gallery", "Unable to delete gallery: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        galleryTable.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
    }

    private Optional<Gallery> showGalleryDialog(Gallery existing) {
        Dialog<Gallery> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Gallery" : "Edit Gallery");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField nameField = new TextField();
        TextField addressField = new TextField();
        TextField ownerField = new TextField();
        TextField hoursField = new TextField();
        TextField phoneField = new TextField();
        TextField ratingField = new TextField();
        TextField websiteField = new TextField();

        if (existing != null) {
            nameField.setText(existing.getName() != null ? existing.getName() : "");
            nameField.setEditable(false);
            addressField.setText(existing.getAddress() != null ? existing.getAddress() : "");
            ownerField.setText(existing.getOwnerName() != null ? existing.getOwnerName() : "");
            hoursField.setText(existing.getOpeningHours() != null ? existing.getOpeningHours() : "");
            phoneField.setText(existing.getContactPhone() != null ? existing.getContactPhone() : "");
            ratingField.setText(String.valueOf(existing.getRating()));
            websiteField.setText(existing.getWebsite() != null ? existing.getWebsite() : "");
        }

        grid.add(new Label("Name *:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Address:"), 0, 1);
        grid.add(addressField, 1, 1);
        grid.add(new Label("Owner:"), 0, 2);
        grid.add(ownerField, 1, 2);
        grid.add(new Label("Opening Hours:"), 0, 3);
        grid.add(hoursField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Rating (0-5):"), 0, 5);
        grid.add(ratingField, 1, 5);
        grid.add(new Label("Website:"), 0, 6);
        grid.add(websiteField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String name = DialogHelper.emptyToNull(nameField.getText());
            if (name == null) {
                DialogHelper.showError("Validation", "Name is required.");
                return null;
            }
            Gallery gallery = existing != null ? existing : new Gallery();
            gallery.setName(name);
            gallery.setAddress(DialogHelper.emptyToNull(addressField.getText()));
            gallery.setOwnerName(DialogHelper.emptyToNull(ownerField.getText()));
            gallery.setOpeningHours(DialogHelper.emptyToNull(hoursField.getText()));
            gallery.setContactPhone(DialogHelper.emptyToNull(phoneField.getText()));
            gallery.setRating(DialogHelper.parseDoubleOrZero(ratingField.getText()));
            gallery.setWebsite(DialogHelper.emptyToNull(websiteField.getText()));
            return gallery;
        });

        return dialog.showAndWait();
    }
}
