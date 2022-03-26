package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class IDCommand extends Command {

    public IDCommand(Linux4Bot instance) {
        super(instance, "id");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text;

        if (message.getChat().isUserChat()) {
            text = "Your ID: <code>" + message.getFrom().getId() + "</code>";
        } else {
            text = "Group ID: <code>" + message.getChatId() + "</code>";

            User user = instance.getUserRef(message);
            if (user != null) text = (user.getUserName() != null ? user.getUserName() : user.getFirstName())
                    + "'s ID: <code>" + user.getId() + "</code>";
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setParseMode("HTML");
        instance.execute(sm);
    }
}
