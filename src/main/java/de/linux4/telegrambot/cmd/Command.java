package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;
import java.util.List;

public abstract class Command {

    protected final Linux4Bot instance;
    private final List<String> commands;

    public Command(Linux4Bot instance, String... commands) {
        this.instance = instance;
        this.commands = Arrays.asList(commands);
    }

    public List<String> getCommands() {
        return commands;
    }

    public abstract void execute(String command, Message message) throws TelegramApiException;

}
