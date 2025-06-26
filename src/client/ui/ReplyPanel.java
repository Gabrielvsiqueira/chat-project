package client.ui;

import javax.swing.*;
import java.awt.*;

public class ReplyPanel extends JPanel {
    private final JTextField replyTopicIdField;
    private final JTextArea replyMessageArea;
    private final JButton sendReplyButton;
    private final JTextField getRepliesTopicIdField;
    private final JButton getRepliesButton;

    public ReplyPanel() {
        super(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Topic ID to Reply:"), gbc);
        replyTopicIdField = new JTextField(10);
        gbc.gridx = 1; gbc.weightx = 1.0; add(replyTopicIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; add(new JLabel("Reply Content:"), gbc);
        replyMessageArea = new JTextArea(3, 20);
        replyMessageArea.setLineWrap(true);
        replyMessageArea.setWrapStyleWord(true);
        JScrollPane replyScrollPane = new JScrollPane(replyMessageArea);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        add(replyScrollPane, gbc);

        sendReplyButton = new JButton("Send Reply (060)");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;
        add(sendReplyButton, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1; gbc.weightx = 0; add(new JLabel("Topic ID to Get Replies:"), gbc);
        getRepliesTopicIdField = new JTextField(10);
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0; add(getRepliesTopicIdField, gbc);

        getRepliesButton = new JButton("Get Replies (070)");
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(getRepliesButton, gbc);
    }

    public String getReplyTopicId() { return replyTopicIdField.getText().trim(); }
    public String getReplyMessage() { return replyMessageArea.getText().trim(); }
    public String getRepliesTopicId() { return getRepliesTopicIdField.getText().trim(); }
    public JButton getSendReplyButton() { return sendReplyButton; }
    public JButton getGetRepliesButton() { return getRepliesButton; }

    public void updateState(boolean isLoggedIn) {
        replyTopicIdField.setEnabled(isLoggedIn);
        replyMessageArea.setEnabled(isLoggedIn);
        sendReplyButton.setEnabled(isLoggedIn);
        getRepliesTopicIdField.setEnabled(isLoggedIn);
        getRepliesButton.setEnabled(isLoggedIn);
    }

    public void clearReplyFields() {
        replyTopicIdField.setText("");
        replyMessageArea.setText("");
    }
}