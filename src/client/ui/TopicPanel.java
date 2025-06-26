package client.ui;

import javax.swing.*;
import java.awt.*;

public class TopicPanel extends JPanel {
    private final JTextField topicTitleField;
    private final JTextField topicSubjectField;
    private final JTextArea topicMessageArea;
    private final JButton createTopicButton;

    public TopicPanel() {
        super(new BorderLayout(5, 5));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("Title:"), gbc);
        topicTitleField = new JTextField(20);
        gbc.gridx = 1; gbc.weightx = 1.0; inputPanel.add(topicTitleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(new JLabel("Subject:"), gbc);
        topicSubjectField = new JTextField(20);
        gbc.gridx = 1; gbc.weightx = 1.0; inputPanel.add(topicSubjectField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        inputPanel.add(new JLabel("Message Content:"), gbc);

        topicMessageArea = new JTextArea(5, 20);
        topicMessageArea.setLineWrap(true);
        topicMessageArea.setWrapStyleWord(true);
        JScrollPane messageScrollPane = new JScrollPane(topicMessageArea);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        inputPanel.add(messageScrollPane, gbc);

        add(inputPanel, BorderLayout.CENTER);

        createTopicButton = new JButton("Create Topic (050)");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(createTopicButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public String getTitleText() { return topicTitleField.getText().trim(); }
    public String getSubjectText() { return topicSubjectField.getText().trim(); }
    public String getMessageText() { return topicMessageArea.getText().trim(); }
    public JButton getCreateTopicButton() { return createTopicButton; }

    public void updateState(boolean isLoggedIn) {
        topicTitleField.setEnabled(isLoggedIn);
        topicSubjectField.setEnabled(isLoggedIn);
        topicMessageArea.setEnabled(isLoggedIn);
        createTopicButton.setEnabled(isLoggedIn);
    }

    public void clearFields() {
        topicTitleField.setText("");
        topicSubjectField.setText("");
        topicMessageArea.setText("");
    }
}