package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.Optional;

public class CommunityController {
    @FXML
    private TableView<CommunityMember> memberTable;
    @FXML
    private TableColumn<CommunityMember, String> nameColumn;
    @FXML
    private TableColumn<CommunityMember, String> emailColumn;
    @FXML
    private TableColumn<CommunityMember, String> cityColumn;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        refreshTable();
    }

    @FXML
    private void handleAdd() {
        Optional<CommunityMember> result = showMemberDialog(null);
        if (result.isPresent()) {
            try {
                communityService.createMember(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Add Member", "Unable to add member: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleEdit() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Edit Member", "Please select a member to edit.");
            return;
        }
        Optional<CommunityMember> result = showMemberDialog(selected);
        if (result.isPresent()) {
            try {
                communityService.updateMember(result.get());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Edit Member", "Unable to update member: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        CommunityMember selected = memberTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogHelper.showError("Delete Member", "Please select a member to delete.");
            return;
        }
        if (DialogHelper.confirm("Delete Member", "Delete member '" + selected.getName() + "'?")) {
            try {
                communityService.deleteMember(selected.getName());
                refreshTable();
            } catch (RuntimeException e) {
                DialogHelper.showError("Delete Member", "Unable to delete member: " + e.getMessage());
            }
        }
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    private Optional<CommunityMember> showMemberDialog(CommunityMember existing) {
        Dialog<CommunityMember> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Member" : "Edit Member");
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField cityField = new TextField();
        TextField birthYearField = new TextField();
        TextField phoneField = new TextField();
        ComboBox<String> membershipBox = new ComboBox<>(
                FXCollections.observableArrayList("Free", "Premium"));

        if (existing != null) {
            nameField.setText(existing.getName() != null ? existing.getName() : "");
            nameField.setEditable(false);
            emailField.setText(existing.getEmail() != null ? existing.getEmail() : "");
            cityField.setText(existing.getCity() != null ? existing.getCity() : "");
            birthYearField.setText(existing.getBirthYear() != null ? String.valueOf(existing.getBirthYear()) : "");
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
            membershipBox.setValue(existing.getMembershipType());
        } else {
            membershipBox.setValue("Free");
        }

        grid.add(new Label("Name *:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email *:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("City:"), 0, 2);
        grid.add(cityField, 1, 2);
        grid.add(new Label("Birth Year:"), 0, 3);
        grid.add(birthYearField, 1, 3);
        grid.add(new Label("Phone:"), 0, 4);
        grid.add(phoneField, 1, 4);
        grid.add(new Label("Membership:"), 0, 5);
        grid.add(membershipBox, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button != saveButtonType) {
                return null;
            }
            String name = DialogHelper.emptyToNull(nameField.getText());
            String email = DialogHelper.emptyToNull(emailField.getText());
            if (name == null) {
                DialogHelper.showError("Validation", "Name is required.");
                return null;
            }
            if (email == null) {
                DialogHelper.showError("Validation", "Email is required.");
                return null;
            }
            CommunityMember member = existing != null ? existing : new CommunityMember();
            member.setName(name);
            member.setEmail(email);
            member.setCity(DialogHelper.emptyToNull(cityField.getText()));
            member.setBirthYear(DialogHelper.parseIntOrNull(birthYearField.getText()));
            member.setPhone(DialogHelper.emptyToNull(phoneField.getText()));
            member.setMembershipType(membershipBox.getValue());
            return member;
        });

        return dialog.showAndWait();
    }
}
