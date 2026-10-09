package com.ragsmc.launcher.auth;

import com.ragsmc.launcher.config.Settings;
import com.ragsmc.launcher.config.SettingsPersistence;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuthManager {

    private final SettingsPersistence persistence;
    private final List<Account> accounts = new ArrayList<>();

    public AuthManager(SettingsPersistence persistence) {
        this.persistence = persistence;
        load();
    }

    private void load() {
        accounts.clear();
        Settings s = persistence.load();
        if (s.accounts != null) {
            for (Settings.AccountEntry e : s.accounts) {
                Account a = new Account(e.id, e.username, e.uuid);
                a.setType(e.type != null ? e.type : "offline");
                a.setSelected(e.selected);
                accounts.add(a);
            }
        }
    }

    public void save() {
        try {
            Settings s = persistence.load();
            List<Settings.AccountEntry> entries = new ArrayList<>();
            for (Account a : accounts) {
                Settings.AccountEntry e = new Settings.AccountEntry();
                e.id = a.getId();
                e.username = a.getUsername();
                e.uuid = a.getUuid();
                e.type = a.getType();
                e.selected = a.isSelected();
                entries.add(e);
            }
            s.accounts = entries;
            persistence.save(s);
        } catch (Exception ignored) {
        }
    }

    public Account addOffline(String username) {
        String clean = username == null ? "" : username.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Nombre vacio");
        }
        for (Account a : accounts) {
            a.setSelected(false);
        }
        String uuid = OfflineUuid.generate(clean);
        Account a = new Account(UUID.randomUUID().toString(), clean, uuid);
        a.setSelected(true);
        accounts.add(a);
        save();
        return a;
    }

    public Account selected() {
        for (Account a : accounts) {
            if (a.isSelected()) {
                return a;
            }
        }
        return accounts.isEmpty() ? null : accounts.get(0);
    }

    public List<Account> all() {
        return new ArrayList<>(accounts);
    }

    public void rename(String id, String newName) {
        String clean = newName == null ? "" : newName.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Nombre vacio");
        }
        boolean found = false;
        for (Account a : accounts) {
            if (a.getId().equals(id)) {
                a.setUsername(clean);
                a.setUuid(OfflineUuid.generate(clean));
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Cuenta no encontrada");
        }
        save();
    }

    public void delete(String id) {
        Account target = null;
        for (Account a : accounts) {
            if (a.getId().equals(id)) {
                target = a;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("Cuenta no encontrada");
        }
        boolean wasSelected = target.isSelected();
        accounts.remove(target);
        if (wasSelected && !accounts.isEmpty()) {
            accounts.get(0).setSelected(true);
        }
        save();
    }

    public void select(String id) {
        for (Account a : accounts) {
            a.setSelected(a.getId().equals(id));
        }
        save();
    }
}