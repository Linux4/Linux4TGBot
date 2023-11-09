package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.GPT4All;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;

public class AskCommand extends Command {

    public AskCommand(Linux4Bot instance) {
        super(instance, "ask");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_AI;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("", "Ask GPT4All a question\\.");
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text = "Text required";
        if (message.hasText()) {
            text = "Thinking...";
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setReplyToMessageId(message.getMessageId());
        int editId = instance.execute(sm).getMessageId();

        if (message.hasText()) {
            text = instance.gpt4All.sendMessage(message.getText().substring("/".length() + command.length()));
            if (text == null || text.isEmpty()) {
                text = "(Empty response from GPT4All)";
            }

            EditMessageText em = new EditMessageText(text);
            em.setChatId(message.getChatId().toString());
            em.setMessageId(editId);
            instance.execute(em);
        }
    }
}
