package client.ui;

import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JPanel {
    private final JTextField adminTargetUserField;
    private final JTextField adminNewNickField;
    private final JPasswordField adminNewPassField;
    private final JButton adminUpdateUserButton;
    private final JButton adminDeleteUserButton;
    private final JTextField adminDeleteMessageIdField;
    private final JButton adminDeleteMessageButton;
    private final JButton adminListAllUsersButton;

    public AdminPanel() {
        super(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Target User (Admin):"), gbc);
        adminTargetUserField = new JTextField(15);
        gbc.gridx = 1; gbc.weightx = 1.0; add(adminTargetUserField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("New Nick (Admin):"), gbc);
        adminNewNickField = new JTextField(15);
        gbc.gridx = 1; add(adminNewNickField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("New Pass (Admin):"), gbc);
        adminNewPassField = new JPasswordField(15);
        gbc.gridx = 1; add(adminNewPassField, gbc);

        adminUpdateUserButton = new JButton("Update User (Admin) (080)");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; add(adminUpdateUserButton, gbc);

        adminDeleteUserButton = new JButton("Delete User (Admin) (090)");
        gbc.gridx = 0; gbc.gridy = 4; add(adminDeleteUserButton, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; add(new JLabel("Message/Topic ID to Delete:"), gbc);
        adminDeleteMessageIdField = new JTextField(10);
        gbc.gridx = 1; gbc.gridy = 6; add(adminDeleteMessageIdField, gbc);

        adminDeleteMessageButton = new JButton("Delete Message (Admin) (100)");
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; add(adminDeleteMessageButton, gbc);

        adminListAllUsersButton = new JButton("List All Users (Admin) (110)");
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; add(adminListAllUsersButton, gbc);
    }

    public String getTargetUser() { return adminTargetUserField.getText().trim(); }
    public String getNewNick() { return adminNewNickField.getText().trim(); }
    public char[] getNewPass() { return adminNewPassField.getPassword(); }
    public String getDeleteMessageId() { return adminDeleteMessageIdField.getText().trim(); }

    public JButton getUpdateUserButton() { return adminUpdateUserButton; }
    public JButton getDeleteUserButton() { return adminDeleteUserButton; }
    public JButton getDeleteMessageButton() { return adminDeleteMessageButton; }
    public JButton getListAllUsersButton() { return adminListAllUsersButton; }

    public void updateState(boolean isAdmin) {
        adminTargetUserField.setEnabled(isAdmin);
        adminNewNickField.setEnabled(isAdmin);
        adminNewPassField.setEnabled(isAdmin);
        adminUpdateUserButton.setEnabled(isAdmin);
        adminDeleteUserButton.setEnabled(isAdmin);
        adminDeleteMessageIdField.setEnabled(isAdmin);
        adminDeleteMessageButton.setEnabled(isAdmin);
        adminListAllUsersButton.setEnabled(isAdmin);
    }

    public void clearUpdateFields() {
        adminTargetUserField.setText("");
        adminNewNickField.setText("");
        adminNewPassField.setText("");
    }
}