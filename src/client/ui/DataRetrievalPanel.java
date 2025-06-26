package client.ui;

import javax.swing.*;
import java.awt.*;

public class DataRetrievalPanel extends JPanel {
    private final JTextField usernameToRetrieveField;
    private final JButton retrieveUserDataButton;
    private final JButton getAllTopicsButton;

    public DataRetrievalPanel() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Data Retrieval"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        add(new JLabel("Username to Retrieve:"), gbc);

        usernameToRetrieveField = new JTextField(10);
        gbc.gridx = 1; gbc.weightx = 1.0;
        add(usernameToRetrieveField, gbc);

        retrieveUserDataButton = new JButton("Get User Data (005)");
        gbc.gridx = 2; gbc.weightx = 0;
        add(retrieveUserDataButton, gbc);

        getAllTopicsButton = new JButton("Get All Topics (075)");
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        add(getAllTopicsButton, gbc);
    }

    public String getUsernameToRetrieve() {
        return usernameToRetrieveField.getText().trim();
    }

    public JButton getRetrieveUserDataButton() {
        return retrieveUserDataButton;
    }

    public JButton getGetAllTopicsButton() {
        return getAllTopicsButton;
    }

    public void updateState(boolean isLoggedIn) {
        usernameToRetrieveField.setEnabled(isLoggedIn);
        retrieveUserDataButton.setEnabled(isLoggedIn);
        getAllTopicsButton.setEnabled(isLoggedIn);
    }
}