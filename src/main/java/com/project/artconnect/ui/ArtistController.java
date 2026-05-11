package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class ArtistController {
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Discipline> disciplineFilter;
    @FXML
    private TableView<Artist> artistTable;
    @FXML
    private TableColumn<Artist, String> nameColumn;
    @FXML
    private TableColumn<Artist, String> cityColumn;
    @FXML
    private TableColumn<Artist, String> emailColumn;
    @FXML
    private TableColumn<Artist, Integer> yearColumn;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline d = disciplineFilter.getValue();
        String dName = (d != null) ? d.getName() : null;
        artistTable.setItems(FXCollections.observableArrayList(artistService.searchArtists(query, dName, null)));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<Artist> result = showArtistDialog(null);
        if (result.isPresent()) {
            try {
                artistService.createArtist(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Artist", "Unable to add artist: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Artist", "Please select an artist to edit.");
            return;
        }
        Optional<Artist> result = showArtistDialog(selected);
        if (result.isPresent()) {
            try {
                artistService.updateArtist(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Artist", "Unable to update artist: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Artist selected = artistTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Artist", "Please select an artist to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Artist", "Delete artist '" + selected.getName() + "'?")) {
            try {
                artistService.deleteArtist(selected.getName());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Artist", "Unable to delete artist: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    private Optional<Artist> showArtistDialog(Artist existing) {
        Dialog<Artist> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Artist" : "Edit Artist");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField nameField = new TextField();
        TextField cityField = new TextField();
        TextField emailField = new TextField();
        TextField yearField = new TextField();
        TextField bioField = new TextField();
        TextField phoneField = new TextField();

        if (existing != null) {
            nameField.setText(existing.getName() != null ? existing.getName() : "");
            nameField.setEditable(false); // name is the business key, cannot change
            cityField.setText(existing.getCity() != null ? existing.getCity() : "");
            emailField.setText(existing.getContactEmail() != null ? existing.getContactEmail() : "");
            yearField.setText(existing.getBirthYear() != null ? String.valueOf(existing.getBirthYear()) : "");
            bioField.setText(existing.getBio() != null ? existing.getBio() : "");
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
        }

        grid.add(new Label("Name *:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("City:"), 0, 1);
        grid.add(cityField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Birth Year:"), 0, 3);
        grid.add(yearField, 1, 3);
        grid.add(new Label("Bio:"), 0, 4);
        grid.add(bioField, 1, 4);
        grid.add(new Label("Phone:"), 0, 5);
        grid.add(phoneField, 1, 5);

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
            Artist artist = existing != null ? existing : new Artist();
            artist.setName(name);
            artist.setCity(DialogHelper.emptyToNull(cityField.getText()));
            artist.setContactEmail(DialogHelper.emptyToNull(emailField.getText()));
            artist.setBirthYear(DialogHelper.parseIntOrNull(yearField.getText()));
            artist.setBio(DialogHelper.emptyToNull(bioField.getText()));
            artist.setPhone(DialogHelper.emptyToNull(phoneField.getText()));
            artist.setActive(true);
            return artist;
        });

        return dialog.showAndWait();
    }
}
