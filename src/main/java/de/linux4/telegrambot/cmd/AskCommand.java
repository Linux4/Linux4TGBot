package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.GPT4All;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
            text = instance.gpt4All.sendMessage(message.getText());
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setReplyToMessageId(message.getMessageId());
        instance.execute(sm);
    }
}
