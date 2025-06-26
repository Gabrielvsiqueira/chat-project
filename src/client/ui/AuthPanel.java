package client.ui;

import javax.swing.*;
import java.awt.*;

public class AuthPanel extends JPanel {
    private final JTextField serverHostField;
    private final JTextField serverPortField;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField nicknameField;
    private final JPasswordField newPasswordField;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JButton logoutButton;
    private final JButton updateProfileButton;
    private final JButton deleteAccountButton;

    public AuthPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Server Connection & Authentication"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Server Host:"), gbc);
        serverHostField = new JTextField("localhost", 15);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; add(serverHostField, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; add(new JLabel("Port:"), gbc);
        serverPortField = new JTextField("12345", 8);
        gbc.gridx = 3; add(serverPortField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Username:"), gbc);
        usernameField = new JTextField(15);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; add(usernameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; add(new JLabel("Password:"), gbc);
        passwordField = new JPasswordField(15);
        gbc.gridx = 3; add(passwordField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Nickname (Register):"), gbc);
        nicknameField = new JTextField(15);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; add(nicknameField, gbc);

        gbc.gridx = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; add(new JLabel("New Password (Update):"), gbc);
        newPasswordField = new JPasswordField(15);
        gbc.gridx = 3; add(newPasswordField, gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        loginButton = new JButton("Login (000)");
        registerButton = new JButton("Register (010)");
        logoutButton = new JButton("Logout (020)");
        updateProfileButton = new JButton("Update Profile (030)");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(logoutButton);
        buttonPanel.add(updateProfileButton);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(buttonPanel, gbc);

        deleteAccountButton = new JButton("Delete Account (040)");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        add(deleteAccountButton, gbc);
    }

    // --- Getters para os dados e botões ---
    public String getHost() { return serverHostField.getText().trim(); }
    public String getPort() { return serverPortField.getText().trim(); }
    public String getUsername() { return usernameField.getText().trim(); }
    public char[] getPassword() { return passwordField.getPassword(); }
    public String getNickname() { return nicknameField.getText().trim(); }
    public char[] getNewPassword() { return newPasswordField.getPassword(); }

    public JButton getLoginButton() { return loginButton; }
    public JButton getRegisterButton() { return registerButton; }
    public JButton getLogoutButton() { return logoutButton; }
    public JButton getUpdateProfileButton() { return updateProfileButton; }
    public JButton getDeleteAccountButton() { return deleteAccountButton; }

    public void updateState(boolean isLoggedIn) {
        serverHostField.setEnabled(!isLoggedIn);
        serverPortField.setEnabled(!isLoggedIn);
        usernameField.setEnabled(!isLoggedIn);
        passwordField.setEnabled(!isLoggedIn);
        loginButton.setEnabled(!isLoggedIn);
        registerButton.setEnabled(!isLoggedIn);
        logoutButton.setEnabled(isLoggedIn);
        updateProfileButton.setEnabled(isLoggedIn);
        deleteAccountButton.setEnabled(isLoggedIn);
    }

    public void clearRegisterFields() {
        usernameField.setText("");
        passwordField.setText("");
        nicknameField.setText("");
    }
}