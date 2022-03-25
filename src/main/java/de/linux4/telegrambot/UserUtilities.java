package de.linux4.telegrambot;

import com.mysql.cj.protocol.ResultStreamer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserUtilities {

    protected static void init(Linux4Bot instance) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Users (UserID Long, Name varchar(255))");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Long getUserId(Linux4Bot instance, String name) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("SELECT UserID FROM Users WHERE Name LIKE ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getLong("UserID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void setUserName(Linux4Bot instance, Long userid, String username) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("DELETE FROM Users WHERE UserID = " + userid);
            ps.executeUpdate();
            ps = instance.mysql.prepareStatement("INSERT INTO Users (UserID, Name) VALUES (" + userid + ", ?)");
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
