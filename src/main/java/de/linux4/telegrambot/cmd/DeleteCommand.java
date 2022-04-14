package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class DeleteCommand extends Command {

    public DeleteCommand(Linux4Bot instance) {
        super(instance, "delete");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_MISC;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("", "Delete the message you replied to\\.");
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (message.getReplyToMessage() != null) {
            DeleteMessage dm = DeleteMessage.builder().chatId(message.getChatId().toString())
                    .messageId(message.getReplyToMessage().getMessageId()).build();
            instance.execute(dm);
            dm = DeleteMessage.builder().chatId(message.getChatId().toString())
                    .messageId(message.getMessageId()).build();
            instance.execute(dm);
        } else {
            SendMessage sm = new SendMessage(message.getChatId().toString(), "Message required!");
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
