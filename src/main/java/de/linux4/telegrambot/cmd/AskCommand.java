package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.ChatGPT;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
        return new HelpInfo("", "Ask AI a question\\.");
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        instance.chatGpt.enqueue(new ChatGPT.Request(message, command));
    }
}
