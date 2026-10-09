package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.auth.Account;
import com.ragsmc.launcher.ui.theme.Colors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ProfilesView extends JPanel {

    private JLabel avatarLabel;
    private JLabel currentName;
    private JLabel currentUuid;
    private JTextField nameField;
    private JTextField addField;
    private final JPanel accountsPanel;
    private List<Account> accounts = new ArrayList<>();
    private Consumer<String> onSaveName;
    private Consumer<String> onCreate;
    private Consumer<String> onSelect;
    private Consumer<String> onDelete;

    public ProfilesView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Colors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        JLabel title = new JLabel("PERFILES", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Colors.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Colors.BACKGROUND);
        center.add(currentCard());
        center.add(Box.createRigidArea(new Dimension(0, 12)));
        center.add(addCard());
        center.add(Box.createRigidArea(new Dimension(0, 12)));
        JLabel listTitle = new JLabel("CUENTAS");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        listTitle.setForeground(Colors.TEXT_PRIMARY);
        listTitle.setAlignmentX(LEFT_ALIGNMENT);
        center.add(listTitle);
        center.add(Box.createRigidArea(new Dimension(0, 6)));
        accountsPanel = new JPanel();
        accountsPanel.setLayout(new BoxLayout(accountsPanel, BoxLayout.Y_AXIS));
        accountsPanel.setBackground(Colors.BACKGROUND);
        accountsPanel.setAlignmentX(LEFT_ALIGNMENT);
        center.add(accountsPanel);
        JScrollPane scroll = new JScrollPane(center);
        scroll.setBackground(Colors.BACKGROUND);
        scroll.getViewport().setBackground(Colors.BACKGROUND);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel card() {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(Colors.CARD_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        c.setAlignmentX(LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(560, 320));
        return c;
    }

    private JPanel currentCard() {
        JPanel c = card();
        JLabel head = new JLabel("CUENTA ACTUAL");
        head.setFont(new Font("Segoe UI", Font.BOLD, 12));
        head.setForeground(Colors.TEXT_SECONDARY);
        head.setAlignmentX(LEFT_ALIGNMENT);
        c.add(head);
        c.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setBackground(Colors.CARD_BG);
        row.setAlignmentX(LEFT_ALIGNMENT);
        avatarLabel = new JLabel("?", SwingConstants.CENTER);
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        avatarLabel.setForeground(Color.BLACK);
        avatarLabel.setBackground(Colors.NEON_GREEN);
        avatarLabel.setOpaque(true);
        avatarLabel.setPreferredSize(new Dimension(56, 56));
        avatarLabel.setMaximumSize(new Dimension(56, 56));
        row.add(avatarLabel);
        JPanel idcol = new JPanel();
        idcol.setLayout(new BoxLayout(idcol, BoxLayout.Y_AXIS));
        idcol.setBackground(Colors.CARD_BG);
        currentName = new JLabel("-");
        currentName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        currentName.setForeground(Colors.TEXT_PRIMARY);
        currentName.setAlignmentX(LEFT_ALIGNMENT);
        JLabel badge = new JLabel("OFFLINE");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(Colors.NEON_GREEN);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Colors.NEON_GREEN, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        badge.setAlignmentX(LEFT_ALIGNMENT);
        idcol.add(currentName);
        idcol.add(Box.createRigidArea(new Dimension(0, 4)));
        idcol.add(badge);
        row.add(idcol);
        c.add(row);
        c.add(Box.createRigidArea(new Dimension(0, 10)));
        currentUuid = new JLabel("UUID: -");
        currentUuid.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        currentUuid.setForeground(Colors.TEXT_SECONDARY);
        currentUuid.setAlignmentX(LEFT_ALIGNMENT);
        c.add(currentUuid);
        c.add(Box.createRigidArea(new Dimension(0, 8)));
        JLabel nl = new JLabel("Nombre de jugador:");
        nl.setForeground(Colors.TEXT_SECONDARY);
        nl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nl.setAlignmentX(LEFT_ALIGNMENT);
        c.add(nl);
        JPanel saveRow = new JPanel(new BorderLayout(8, 0));
        saveRow.setBackground(Colors.CARD_BG);
        saveRow.setAlignmentX(LEFT_ALIGNMENT);
        saveRow.setMaximumSize(new Dimension(520, 30));
        nameField = new JTextField();
        nameField.setBackground(Colors.FIELD_BACKGROUND);
        nameField.setForeground(Colors.TEXT_PRIMARY);
        nameField.setCaretColor(Colors.NEON_GREEN);
        saveRow.add(nameField, BorderLayout.CENTER);
        JButton save = new JButton("Guardar nombre");
        save.setBackground(Colors.NEON_GREEN);
        save.setForeground(Color.BLACK);
        save.setFocusPainted(false);
        save.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        save.setCursor(new Cursor(Cursor.HAND_CURSOR));
        save.addActionListener(e -> {
            String n = nameField.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe un nombre.", "Vacio", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (onSaveName != null) {
                onSaveName.accept(n);
            }
        });
        saveRow.add(save, BorderLayout.EAST);
        c.add(saveRow);
        return c;
    }

    private JPanel addCard() {
        JPanel c = card();
        JLabel head = new JLabel("AGREGAR CUENTA");
        head.setFont(new Font("Segoe UI", Font.BOLD, 12));
        head.setForeground(Colors.TEXT_SECONDARY);
        head.setAlignmentX(LEFT_ALIGNMENT);
        c.add(head);
        c.add(Box.createRigidArea(new Dimension(0, 8)));
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(Colors.CARD_BG);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(520, 30));
        addField = new JTextField();
        addField.setBackground(Colors.FIELD_BACKGROUND);
        addField.setForeground(Colors.TEXT_PRIMARY);
        addField.setCaretColor(Colors.NEON_GREEN);
        row.add(addField, BorderLayout.CENTER);
        JButton add = new JButton("+ Agregar");
        add.setBackground(Colors.NEON_GREEN);
        add.setForeground(Color.BLACK);
        add.setFocusPainted(false);
        add.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        add.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add.addActionListener(e -> {
            String n = addField.getText().trim();
            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe un nombre.", "Vacio", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (onCreate != null) {
                onCreate.accept(n);
                addField.setText("");
            }
        });
        row.add(add, BorderLayout.EAST);
        c.add(row);
        return c;
    }

    public void setAccountsList(final List<Account> list) {
        SwingUtilities.invokeLater(() -> {
            accounts = new ArrayList<>(list);
            accountsPanel.removeAll();
            Account current = null;
            for (Account a : list) {
                if (a.isSelected()) {
                    current = a;
                }
                accountsPanel.add(accountRow(a));
                accountsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
            if (current != null) {
                avatarLabel.setText(current.getUsername().substring(0, 1).toUpperCase());
                currentName.setText(current.getUsername());
                currentUuid.setText("UUID: " + current.getUuid());
                nameField.setText(current.getUsername());
            } else {
                avatarLabel.setText("?");
                currentName.setText("-");
                currentUuid.setText("UUID: -");
            }
            accountsPanel.revalidate();
            accountsPanel.repaint();
        });
    }

    private JPanel accountRow(Account a) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(a.isSelected() ? new java.awt.Color(0x0d3324) : Colors.CARD_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(a.isSelected() ? Colors.NEON_GREEN : Colors.BACKGROUND_HOVER, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        row.setMaximumSize(new Dimension(560, 64));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel av = new JLabel(a.getUsername().substring(0, 1).toUpperCase(), SwingConstants.CENTER);
        av.setFont(new Font("Segoe UI", Font.BOLD, 18));
        av.setForeground(Colors.TEXT_PRIMARY);
        av.setBackground(Colors.FIELD_BACKGROUND);
        av.setOpaque(true);
        av.setPreferredSize(new Dimension(40, 40));
        row.add(av, BorderLayout.WEST);
        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setBackground(row.getBackground());
        JLabel name = new JLabel(a.getUsername() + (a.isSelected() ? "   ✓ Activa" : ""));
        name.setFont(new Font("Segoe UI", Font.BOLD, 13));
        name.setForeground(a.isSelected() ? Colors.NEON_GREEN : Colors.TEXT_PRIMARY);
        JLabel off = new JLabel("Offline");
        off.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        off.setForeground(Colors.TEXT_SECONDARY);
        mid.add(name);
        mid.add(off);
        row.add(mid, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setBackground(row.getBackground());
        if (!a.isSelected()) {
            JButton sel = new JButton("Seleccionar");
            sel.setBackground(Colors.FIELD_BACKGROUND);
            sel.setForeground(Colors.TEXT_PRIMARY);
            sel.setFocusPainted(false);
            sel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            sel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            sel.addActionListener(e -> {
                if (onSelect != null) {
                    onSelect.accept(a.getId());
                }
            });
            btns.add(sel);
        }
        JButton del = new JButton("X");
        del.setBackground(Colors.FIELD_BACKGROUND);
        del.setForeground(new Color(0xef4444));
        del.setFocusPainted(false);
        del.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        del.setCursor(new Cursor(Cursor.HAND_CURSOR));
        del.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this, "Eliminar cuenta " + a.getUsername() + "?", "Confirmar",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION && onDelete != null) {
                onDelete.accept(a.getId());
            }
        });
        btns.add(del);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    public void setOnSaveName(Consumer<String> r) {
        this.onSaveName = r;
    }

    public void setOnCreate(Consumer<String> r) {
        this.onCreate = r;
    }

    public void setOnSelect(Consumer<String> r) {
        this.onSelect = r;
    }

    public void setOnDelete(Consumer<String> r) {
        this.onDelete = r;
    }

    // Compatibilidad con cableado anterior
    public void setAccounts(final java.util.List<String> displays, final String selected) {
    }
}