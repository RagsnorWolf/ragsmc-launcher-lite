package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.ui.theme.Colors;
import com.ragsmc.launcher.ui.theme.Images;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

public class Sidebar extends JPanel {

    private static final Color HOVER_BG = new Color(0x141a18);
    private final Consumer<String> onNavigate;
    private final Map<String, JButton> buttons = new HashMap<>();
    private String active = "home";

    public Sidebar(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
        setPreferredSize(new Dimension(220, 0));
        setBackground(Colors.BACKGROUND_DARK);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(createLogoHeader());
        add(Box.createRigidArea(new Dimension(0, 10)));
        String[] buttonLabels = {"INICIO", "INSTALACIONES", "RECURSOS", "PERFIL", "AJUSTES"};
        String[] viewNames = {"home", "installations", "resources", "profiles", "settings"};
        for (int i = 0; i < buttonLabels.length; i++) {
            JButton button = createNavButton(buttonLabels[i], viewNames[i]);
            buttons.put(viewNames[i], button);
            add(button);
            add(Box.createRigidArea(new Dimension(0, 2)));
        }
        add(Box.createVerticalGlue());
        add(createUserProfile());
    }

    private JPanel createLogoHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBackground(Colors.BACKGROUND_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        header.setMaximumSize(new Dimension(220, 60));
        header.setAlignmentX(CENTER_ALIGNMENT);
        ImageIcon logo = Images.icon("logo_small.png", 36, 36);
        JLabel icon;
        if (logo != null) {
            icon = new JLabel(logo);
        } else {
            icon = new JLabel("R");
            icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
            icon.setForeground(Colors.NEON_GREEN);
        }
        header.add(icon);
        header.add(Box.createRigidArea(new Dimension(8, 0)));
        JLabel name = new JLabel("RagsMC LAUNCHER");
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(Colors.TEXT_PRIMARY);
        header.add(name);
        return header;
    }

    private JButton createNavButton(String text, String viewName) {
        JButton button = new JButton(text);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 38));
        button.setMinimumSize(new Dimension(200, 38));
        button.setPreferredSize(new Dimension(200, 38));
        button.setBackground(Colors.BACKGROUND_DARK);
        button.setForeground(Colors.TEXT_SECONDARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                if (!active.equals(viewName)) {
                    button.setBackground(HOVER_BG);
                    button.setForeground(Colors.TEXT_PRIMARY);
                }
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent evt) {
                paintState(button, viewName);
            }
            @Override
            public void mousePressed(MouseEvent evt) {
                if (onNavigate != null) {
                    onNavigate.accept(viewName);
                }
            }
        });
        button.addActionListener(e -> {
            if (onNavigate != null) {
                onNavigate.accept(viewName);
            }
        });
        return button;
    }

    private void paintState(JButton button, String viewName) {
        if (active.equals(viewName)) {
            button.setBackground(Colors.NEON_GREEN);
            button.setForeground(Color.BLACK);
        } else {
            button.setBackground(Colors.BACKGROUND_DARK);
            button.setForeground(Colors.TEXT_SECONDARY);
        }
    }

    private JPanel createUserProfile() {
        JPanel profilePanel = new JPanel();
        profilePanel.setBackground(Colors.BACKGROUND_DARK);
        profilePanel.setMaximumSize(new Dimension(200, 90));
        profilePanel.setMinimumSize(new Dimension(200, 90));
        profilePanel.setPreferredSize(new Dimension(200, 90));
        profilePanel.setAlignmentX(CENTER_ALIGNMENT);
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));
        ImageIcon avatarIcon = Images.icon("logo_small.png", 28, 28);
        JLabel avatar;
        if (avatarIcon != null) {
            avatar = new JLabel(avatarIcon);
        } else {
            avatar = new JLabel("R");
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 24));
            avatar.setForeground(Colors.NEON_GREEN);
        }
        avatar.setAlignmentX(CENTER_ALIGNMENT);
        JLabel username = new JLabel("RagsnorWolf");
        username.setName("sidebarUsername");
        username.setForeground(Colors.TEXT_PRIMARY);
        username.setAlignmentX(CENTER_ALIGNMENT);
        username.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel status = new JLabel("En linea");
        status.setForeground(Colors.NEON_GREEN);
        status.setAlignmentX(CENTER_ALIGNMENT);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        profilePanel.add(avatar);
        profilePanel.add(username);
        profilePanel.add(status);
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        sep.setForeground(Colors.TEXT_SECONDARY);
        profilePanel.add(sep);
        return profilePanel;
    }

    public void setActiveView(String viewName) {
        String key = viewName != null ? viewName : "";
        if (key.equals("play") || key.equals("console") || key.equals("mods") || key.equals("shaders")) {
            key = "home";
        }
        this.active = key;
        for (Map.Entry<String, JButton> entry : buttons.entrySet()) {
            paintState(entry.getValue(), entry.getKey());
        }
    }

    public void setUsername(String username) {
        for (java.awt.Component c : getComponents()) {
            updateUsernameRecursive(c, username);
        }
    }

    private void updateUsernameRecursive(java.awt.Component c, String username) {
        if (c instanceof JLabel) {
            JLabel l = (JLabel) c;
            if ("sidebarUsername".equals(l.getName())) {
                l.setText(username);
            }
        }
        if (c instanceof java.awt.Container) {
            for (java.awt.Component child : ((java.awt.Container) c).getComponents()) {
                updateUsernameRecursive(child, username);
            }
        }
    }
}