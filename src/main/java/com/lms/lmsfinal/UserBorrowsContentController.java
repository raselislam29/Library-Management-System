package com.lms.lmsfinal;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserBorrowsContentController {

    @FXML private TabPane tabs;

    @FXML private TableView<UserBorrow> borrowedTable;
    @FXML private TableColumn<UserBorrow, String> bIsbn, bTitle, bDue, bAt;
    @FXML private TableColumn<UserBorrow, String> bAction;
    @FXML private TextField searchBorrowed;

    @FXML private TableView<UserBorrow> returnedTable;
    @FXML private TableColumn<UserBorrow, String> rIsbn, rTitle, rDue, rAt;
    @FXML private TextField searchReturned;

    private final FirebaseService firebase = new FirebaseService();

    // Backing lists + filtered views
    private final ObservableList<UserBorrow> borrowedBase  = FXCollections.observableArrayList();
    private final ObservableList<UserBorrow> returnedBase  = FXCollections.observableArrayList();
    private final FilteredList<UserBorrow>  borrowed       = new FilteredList<>(borrowedBase, b -> true);
    private final FilteredList<UserBorrow>  returned       = new FilteredList<>(returnedBase, b -> true);

    @FXML
    private void initialize() {
        // Borrowed columns
        bIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        bTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        bDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        bAt.setCellValueFactory(new PropertyValueFactory<>("borrowedAt"));

        // Return button column
        bAction.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("Return");
            { btn.getStyleClass().add("button"); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                UserBorrow row = getTableView().getItems().get(getIndex());
                btn.setOnAction(e -> doReturn(row));
                setGraphic(btn);
            }
        });

        // Returned columns
        rIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        rTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        rDue.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        rAt.setCellValueFactory(new PropertyValueFactory<>("returnedAt"));

        // Hook data
        borrowedTable.setItems(borrowed);
        returnedTable.setItems(returned);

        // Search bindings
        searchBorrowed.textProperty().addListener((obs, o, n) ->
                borrowed.setPredicate(u -> filter(u, n)));
        searchReturned.textProperty().addListener((obs, o, n) ->
                returned.setPredicate(u -> filter(u, n)));

        load();
    }

    private boolean filter(UserBorrow u, String q) {
        if (q == null || q.isBlank()) return true;
        q = q.toLowerCase();
        return (u.getTitle() != null && u.getTitle().toLowerCase().contains(q))
                || (u.getIsbn()  != null && u.getIsbn().toLowerCase().contains(q));
    }

    private void load() {
        final String email = Session.email;
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                List<UserBorrow> act = firebase.getBorrowsForUser(email, false);
                List<UserBorrow> ret = firebase.getBorrowsForUser(email, true);
                Platform.runLater(() -> {
                    borrowedBase.setAll(act);   // ✅ set on backing list
                    returnedBase.setAll(ret);   // ✅ set on backing list
                });
                return null;
            }
        };
        new Thread(t, "user-borrows-load").start();
    }

    private void doReturn(UserBorrow row) {
        if (row == null) return;
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                try {
                    firebase.returnBook(row.getIsbn(), Session.email);
                    // reload after returning
                    List<UserBorrow> act = firebase.getBorrowsForUser(Session.email, false);
                    List<UserBorrow> ret = firebase.getBorrowsForUser(Session.email, true);
                    Platform.runLater(() -> {
                        borrowedBase.setAll(act);   // ✅ update backing list
                        returnedBase.setAll(ret);   // ✅ update backing list
                    });
                } catch (Exception e) { e.printStackTrace(); }
                return null;
            }
        };
        new Thread(t, "user-borrows-return").start();
    }
}
