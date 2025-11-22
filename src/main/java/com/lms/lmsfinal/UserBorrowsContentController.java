package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserBorrowsContentController {

    @FXML private TabPane tabs;

    @FXML private TextField searchBorrowed;
    @FXML private TableView<UserBorrow> borrowedTable;
    @FXML private TableColumn<UserBorrow, String> bIsbn;
    @FXML private TableColumn<UserBorrow, String> bTitle;
    @FXML private TableColumn<UserBorrow, String> bDue;
    @FXML private TableColumn<UserBorrow, String> bAt;
    @FXML private TableColumn<UserBorrow, Void>   bAction;

    @FXML private TextField searchReturned;
    @FXML private TableView<UserBorrow> returnedTable;
    @FXML private TableColumn<UserBorrow, String> rIsbn;
    @FXML private TableColumn<UserBorrow, String> rTitle;
    @FXML private TableColumn<UserBorrow, String> rDue;
    @FXML private TableColumn<UserBorrow, String> rAt;

    private final FirebaseService firebase = new FirebaseService();

    // Master lists
    private final ObservableList<UserBorrow> allBorrowed  = FXCollections.observableArrayList();
    private final ObservableList<UserBorrow> allReturned  = FXCollections.observableArrayList();

    // Filtered lists shown in tables
    private final ObservableList<UserBorrow> borrowedFiltered = FXCollections.observableArrayList();
    private final ObservableList<UserBorrow> returnedFiltered = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Setup borrowed columns
        bIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        bTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        bDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        bAt.setCellValueFactory(new PropertyValueFactory<>("borrowedAt"));

        // Setup returned columns
        rIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        rTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        rDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        rAt.setCellValueFactory(new PropertyValueFactory<>("returnedAt"));

        // Attach filtered lists to tables
        borrowedTable.setItems(borrowedFiltered);
        returnedTable.setItems(returnedFiltered);

        // Action column (Return button on active borrows)
        setupReturnButtonColumn();

        // Search filters: update as user types
        searchBorrowed.textProperty().addListener((obs, oldV, newV) -> applyBorrowedFilter());
        searchReturned.textProperty().addListener((obs, oldV, newV) -> applyReturnedFilter());

        // Load data initially
        refreshData();
    }

    // --------------------------------------------------
    // Load borrows for this user
    // --------------------------------------------------
    private void refreshData() {
        final String userEmail = Session.email;

        if (userEmail == null || userEmail.isBlank()) {
            // No logged-in user: clear tables
            Platform.runLater(() -> {
                allBorrowed.clear();
                allReturned.clear();
                borrowedFiltered.clear();
                returnedFiltered.clear();
            });
            return;
        }

        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Active (not returned)
                    List<UserBorrow> active = firebase.getBorrowsForUser(userEmail, false);
                    // Already returned
                    List<UserBorrow> ret    = firebase.getBorrowsForUser(userEmail, true);

                    Platform.runLater(() -> {
                        allBorrowed.setAll(active);
                        allReturned.setAll(ret);
                        applyBorrowedFilter();
                        applyReturnedFilter();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() ->
                            showError("Failed to load borrows: " + e.getMessage()));
                }
                return null;
            }
        };

        new Thread(t).start();
    }

    // --------------------------------------------------
    // Filtering
    // --------------------------------------------------
    private void applyBorrowedFilter() {
        String q = searchBorrowed.getText() == null ? "" : searchBorrowed.getText().trim().toLowerCase();
        borrowedFiltered.setAll(
                allBorrowed.filtered(ub ->
                        q.isEmpty()
                                || (ub.getTitle() != null && ub.getTitle().toLowerCase().contains(q))
                                || (ub.getIsbn()  != null && ub.getIsbn().toLowerCase().contains(q))
                )
        );
    }

    private void applyReturnedFilter() {
        String q = searchReturned.getText() == null ? "" : searchReturned.getText().trim().toLowerCase();
        returnedFiltered.setAll(
                allReturned.filtered(ub ->
                        q.isEmpty()
                                || (ub.getTitle() != null && ub.getTitle().toLowerCase().contains(q))
                                || (ub.getIsbn()  != null && ub.getIsbn().toLowerCase().contains(q))
                )
        );
    }

    // --------------------------------------------------
    // Return button in "Action" column
    // --------------------------------------------------
    private void setupReturnButtonColumn() {
        bAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Return");

            {
                btn.setOnAction(evt -> {
                    UserBorrow ub = getTableView().getItems().get(getIndex());
                    if (ub == null) return;

                    final String email = Session.email;
                    if (email == null || email.isBlank()) {
                        showError("No logged-in user. Please log in again.");
                        return;
                    }

                    // Confirm dialog
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Return Book");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Return \"" + ub.getTitle() + "\" ?");
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.OK) {
                            doReturnBook(ub.getIsbn(), email);
                        }
                    });
                });
                btn.setStyle("-fx-background-color:#22c55e; -fx-text-fill:white; -fx-background-radius:4;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private void doReturnBook(String isbn, String userEmail) {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                try {
                    firebase.returnBook(isbn, userEmail);
                    Platform.runLater(() -> {
                        showInfo("Book returned.");
                        refreshData();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showError("Error returning book: " + e.getMessage()));
                }
                return null;
            }
        };
        new Thread(t).start();
    }

    // --------------------------------------------------
    // Simple alerts
    // --------------------------------------------------
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Info");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }
}
