package client.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class LogPanel extends JPanel {
    private final JTextArea receivedMessagesArea;

    public LogPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Forum Messages / Client Log"));

        receivedMessagesArea = new JTextArea(15, 0);
        receivedMessagesArea.setEditable(false);
        receivedMessagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(receivedMessagesArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            receivedMessagesArea.append("[" + new Date() + "] " + message + "\n");
            receivedMessagesArea.setCaretPosition(receivedMessagesArea.getDocument().getLength());
        });
    }
}