package com.lms.lmsfinal;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class BorrowFormController {

    @FXML private TextField nameField, studentIdField, phoneField, emailField;
    @FXML private Label errLabel;

    public static BorrowFormData open() {
        try {
            FXMLLoader loader = new FXMLLoader(BorrowFormController.class.getResource("/com/lms/lmsfinal/BorrowForm.fxml"));
            DialogPane pane = loader.load();
            BorrowFormController c = loader.getController();

            Dialog<BorrowFormData> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Borrow Book");

            dialog.setResultConverter(btn -> {
                if (btn.getButtonData().isCancelButton()) return null;
                return c.collect();
            });

            return dialog.showAndWait().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private BorrowFormData collect() {
        String name = val(nameField);
        String sid  = val(studentIdField);
        String ph   = val(phoneField);
        String em   = val(emailField);
        if (name.isBlank() || sid.isBlank() || ph.isBlank() || em.isBlank()) {
            if (errLabel != null) errLabel.setText("All fields are required.");
            return null;
        }
        return new BorrowFormData(name, sid, ph, em);
    }

    private String val(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }
}

class BorrowFormData {
    private final String studentName, studentId, phone, studentEmail;
    public BorrowFormData(String studentName, String studentId, String phone, String studentEmail) {
        this.studentName = studentName; this.studentId = studentId; this.phone = phone; this.studentEmail = studentEmail;
    }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getPhone() { return phone; }
    public String getStudentEmail() { return studentEmail; }
}
