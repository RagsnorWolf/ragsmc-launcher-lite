package com.ragsmc.launcher.ui;

import com.ragsmc.launcher.ui.theme.Colors;
import com.ragsmc.launcher.ui.theme.Images;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;

public class MainWindow extends JFrame {

    private final JPanel contentPanel;
    private final Sidebar sidebar;
    private final RightPanel rightPanel;
    private final CardLayout cardLayout;
    private final HomePlayView homePlayView;
    private final InstallationsView installationsView;
    private final RecursosView recursosView;
    private final ProfilesView profilesView;
    private final SettingsView settingsView;
    private final ConsoleView consoleView;

    public MainWindow() {
        setTitle("RagsMC Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        sidebar = new Sidebar(this::switchView);
        add(sidebar, BorderLayout.WEST);
        contentPanel = new JPanel(new CardLayout());
        cardLayout = (CardLayout) contentPanel.getLayout();
        homePlayView = new HomePlayView();
        installationsView = new InstallationsView(this);
        recursosView = new RecursosView();
        profilesView = new ProfilesView();
        settingsView = new SettingsView();
        consoleView = new ConsoleView();
        contentPanel.add(homePlayView, "home");
        contentPanel.add(installationsView, "installations");
        contentPanel.add(recursosView, "resources");
        contentPanel.add(profilesView, "profiles");
        contentPanel.add(settingsView, "settings");
        contentPanel.add(consoleView, "console");
        add(contentPanel, BorderLayout.CENTER);
        rightPanel = new RightPanel(this::switchView, defaultGameDir());
        add(rightPanel, BorderLayout.EAST);
        add(createFooter(), BorderLayout.SOUTH);
        sidebar.setActiveView("home");
        cardLayout.show(contentPanel, "home");
    }

    private static File defaultGameDir() {
        String appdata = System.getenv("APPDATA");
        if (appdata != null && !appdata.isEmpty()) {
            return new File(appdata + File.separator + ".minecraft");
        }
        return new File(System.getProperty("user.home") + File.separator + ".minecraft");
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Colors.FOOTER_BG);
        footer.setPreferredSize(new Dimension(1, 28));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        JLabel left = new JLabel("Con senal  |  Launcher v1.0.4");
        left.setForeground(Colors.TEXT_SECONDARY);
        left.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.add(left, BorderLayout.WEST);
        JPanel social = new JPanel();
        social.setLayout(new BoxLayout(social, BoxLayout.X_AXIS));
        social.setBackground(Colors.FOOTER_BG);
        JLabel follow = new JLabel("Sigueme en mis redes  ");
        follow.setForeground(Colors.TEXT_SECONDARY);
        follow.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        social.add(follow);
        String[] nets = {"twitch", "tiktok", "whatsapp"};
        for (String net : nets) {
            ImageIcon icon = Images.icon("icons/" + net + ".png", 16, 16);
            JLabel l;
            if (icon != null) {
                l = new JLabel(icon);
            } else {
                l = new JLabel(net);
                l.setForeground(Colors.TEXT_SECONDARY);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            }
            l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            social.add(l);
        }
        footer.add(social, BorderLayout.EAST);
        return footer;
    }

    private String mapCard(String viewName) {
        if (viewName == null) {
            return "home";
        }
        if (viewName.equals("play") || viewName.equals("mods") || viewName.equals("shaders")) {
            return "home";
        }
        return viewName;
    }

    private void switchView(String viewName) {
        if (viewName != null && viewName.equals("play")) {
            homePlayView.scrollToBottom();
        } else if (viewName != null && viewName.equals("home")) {
            homePlayView.scrollToTop();
        }
        cardLayout.show(contentPanel, mapCard(viewName));
        sidebar.setActiveView(viewName);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void showView(String viewName) {
        switchView(viewName);
    }

    public Sidebar getSidebar() {
        return sidebar;
    }

    public RightPanel getRightPanel() {
        return rightPanel;
    }

    public HomePlayView getHomeView() {
        return homePlayView;
    }

    public HomePlayView getPlayView() {
        return homePlayView;
    }

    public InstallationsView getInstallationsView() {
        return installationsView;
    }

    public RecursosView getRecursosView() {
        return recursosView;
    }

    public ProfilesView getProfilesView() {
        return profilesView;
    }

    public SettingsView getSettingsView() {
        return settingsView;
    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }
}