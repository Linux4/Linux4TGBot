package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.GPT4All;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.locks.ReentrantLock;

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
        instance.gpt4All.enqueue(new GPT4All.Request(message, command));
    }
}
