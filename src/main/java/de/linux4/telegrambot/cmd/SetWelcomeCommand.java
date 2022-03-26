package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class SetWelcomeCommand extends Command {

    public SetWelcomeCommand(Linux4Bot instance) {
        super(instance, "setwelcome", "setgoodbye");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Welcome (ChatID Long, Text varchar(4096), Welcome BOOLEAN)").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getWelcomeMessage(Linux4Bot instance, Long chatId, boolean welcome) {
        try {
            ResultSet rs = instance.mysql.prepareStatement("SELECT Text FROM Welcome WHERE ChatID = " + chatId
                + " AND Welcome = " + welcome).executeQuery();
            if (rs.next()) return rs.getString("Text");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            boolean isWelcome = command.equalsIgnoreCase("setwelcome");
            String text = "New " + (isWelcome ? "welcome" : "goodbye") + " message set!";
            String[] broken = message.getText().trim().split(" ");

            if (broken.length > 1) {
                String welcome = Joiner.on(' ').join(Arrays.copyOfRange(broken, 1, broken.length));

                try {
                    instance.mysql.prepareStatement("DELETE FROM Welcome WHERE ChatID = " + message.getChatId()
                        + " AND Welcome = " + isWelcome).executeUpdate();
                    PreparedStatement ps = instance.mysql.prepareStatement("INSERT INTO Welcome (ChatID, Text, Welcome) VALUES ("
                            + message.getChatId() + ", ?, " + isWelcome + ")");
                    ps.setString(1, welcome);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    instance.mysql.prepareStatement("DELETE FROM Welcome WHERE ChatID = " + message.getChatId()
                        + " AND Welcome = " + isWelcome).executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            SendMessage sm = new SendMessage(message.getChatId().toString(), text);
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
