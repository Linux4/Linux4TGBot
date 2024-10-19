package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class SetWelcomeCommand extends Command {

    public SetWelcomeCommand(Linux4Bot instance) {
        super(instance, "setgoodbye", "setwelcome");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Welcome (ChatID Long, Text varchar(4096), Welcome BOOLEAN)").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_GREETINGS;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        switch (command) {
            case "setgoodbye": return new HelpInfo("<text>", "Set a new goodbye message\\." +
                    " Supports markdown, buttons and fillings\\.");
            case "setwelcome": return new HelpInfo("<text>", "Set a new welcome message\\." +
                    " Supports markdown, buttons and fillings\\.");
        }
        return super.getHelpInfo(command);
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
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
        boolean isWelcome = command.equalsIgnoreCase("setwelcome");
        String text = "New " + (isWelcome ? "welcome" : "goodbye") + " message set!";
        String[] broken = message.getReplyToMessage() != null ? message.getReplyToMessage().getText().trim().split(" ")
                : message.getText().trim().split(" ");

        if (broken.length > 1) {
            int startIndex = message.getReplyToMessage() != null ? 0 : 1;
            String welcome = Joiner.on(' ').join(Arrays.copyOfRange(broken, startIndex, broken.length));

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
        instance.telegramClient.execute(sm);
    }
}
