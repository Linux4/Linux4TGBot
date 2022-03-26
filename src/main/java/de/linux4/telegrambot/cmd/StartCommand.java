package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class StartCommand extends Command {

    public StartCommand(Linux4Bot instance) {
        super(instance, "start");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (message.getText().trim().split(" ").length > 1) {
            String startArgs = message.getText().trim().split(" ")[1];

            for (Command botCommand : instance.commands) {
                for (String commandName : botCommand.getCommands()) {
                    if (commandName.equalsIgnoreCase(startArgs.split("_")[0])) {
                        botCommand.execute(startArgs, message);
                        break;
                    }
                }
            }
        }
    }
}
