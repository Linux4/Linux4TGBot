package de.linux4.telegrambot;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Settings {

    public static String KEY_CAPTCHA = "captcha";

    public static void init(Linux4Bot instance) {
        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Settings (ChatID LONG, SettingsKey varchar(255) NOT NULL, " +
                            "SettingsValue varchar(4096) NOT NULL)")
                    .executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void saveSetting(Linux4Bot instance, long chatId, String key, String value) {
        try {
            PreparedStatement ps;
            if (getSetting(instance, chatId, key, null) == null) { // Does not exist yet
                ps = instance.mysql.prepareStatement("INSERT INTO Settings (ChatID, SettingsKey, SettingsValue) VALUES ("
                        + chatId + ", ?, ?)");
            } else {
                ps = instance.mysql.prepareStatement("UPDATE Settings SET SettingsKey = ?, SettingsValue = ? WHERE ChatID = " + chatId);
            }
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static String getSetting(Linux4Bot instance, long chatId, String key, String defValue) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("SELECT SettingsValue FROM Settings WHERE SettingsKey LIKE ? AND ChatID = " + chatId);
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) return rs.getString("SettingsValue");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return defValue;
    }

    public static void saveSettingBool(Linux4Bot instance, long chatId, String key, boolean value) {
        saveSetting(instance, chatId, key, Boolean.toString(value));
    }

    public static boolean getSettingBool(Linux4Bot instance, long chatId, String key, boolean defValue) {
        return Boolean.parseBoolean(getSetting(instance, chatId, key, Boolean.toString(defValue)));
    }

}
