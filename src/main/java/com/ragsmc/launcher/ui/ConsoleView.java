package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.ui.theme.Colors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

public class ConsoleView extends JPanel {

    private final JTextArea consoleArea;
    private final JButton clearButton;

    public ConsoleView() {
        setLayout(new BorderLayout(8, 8));
        setBackground(Colors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(new Color(0x00ff88));
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setText("Consola RagsMC lista.\n");
        JScrollPane scroll = new JScrollPane(consoleArea);
        scroll.setBorder(BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1));
        add(scroll, BorderLayout.CENTER);
        clearButton = new JButton("Limpiar");
        clearButton.setBackground(Colors.FIELD_BACKGROUND);
        clearButton.setForeground(Colors.TEXT_PRIMARY);
        clearButton.setFocusPainted(false);
        clearButton.addActionListener(e -> consoleArea.setText(""));
        add(clearButton, BorderLayout.SOUTH);
    }

    public void appendLine(final String line) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(line + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> consoleArea.setText(""));
    }
}