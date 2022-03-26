package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class SetRulesCommand extends Command {

    public SetRulesCommand(Linux4Bot instance) {
        super(instance, "setrules");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Rules (ChatID LONG, Text varchar(4096), Entities varchar(4096))").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            String text = "Rules set!";
            String[] broken = message.getText().trim().split(" ");

            try {
                instance.mysql.prepareStatement("DELETE FROM Rules WHERE ChatID = " + message.getChatId()).executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (broken.length > 1 || message.getReplyToMessage() != null) {
                String rules;
                if (broken.length > 1)
                    rules = Joiner.on(' ').join(Arrays.copyOfRange(broken, 1, broken.length));
                else
                    rules = message.getReplyToMessage().getText();

                try {
                    PreparedStatement ps = instance.mysql.prepareStatement("INSERT INTO Rules (ChatID, Text, Entities) VALUES ("
                            + message.getChatId() + ", ?, ?)");
                    ps.setString(1, rules);
                    if (message.getReplyToMessage() != null)
                        ps.setString(2, MessageUtilities.entitiesToString(message.getReplyToMessage().getEntities(),
                                0));
                    else
                        ps.setString(2, MessageUtilities.entitiesToString(message.getEntities(),
                                rules.length() - message.getText().length()));
                    ps.executeUpdate();
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
