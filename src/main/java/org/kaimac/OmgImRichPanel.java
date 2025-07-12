package org.kaimac;

import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class OmgImRichPanel extends PluginPanel {
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> list = new JList<>(listModel);
    private final JLabel statusLabel = new JLabel();

    public OmgImRichPanel() {
        setLayout(new BorderLayout());
        list.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(list);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 16));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(statusLabel, BorderLayout.NORTH);
    }

    public void updateResults(List<String> results) {
        listModel.clear();
        for (String result : results) {
            listModel.addElement(result);
        }
    }

    public void setMessage(String message) {
        statusLabel.setText(message);
    }

    public void clearMessage() {
        statusLabel.setText("");
    }
}
