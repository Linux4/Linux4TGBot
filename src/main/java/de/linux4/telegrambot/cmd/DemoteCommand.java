package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class DemoteCommand extends Command {

    public DemoteCommand(Linux4Bot instance) {
        super(instance, "demote");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_ADMIN;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("<reply\\/username\\/mention\\/userid>", "Demote a user\\.");
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        User user = instance.getUserRef(message);
        List<MessageEntity> entities = new ArrayList<>();
        String text = "User required!";

        if (user != null) {
            text = "User isn't Admin in this group!";
            GetChatAdministrators administrators = GetChatAdministrators.builder()
                    .chatId(message.getChatId().toString()).build();
            for (ChatMember member : instance.execute(administrators)) {
                if (member.getUser().getId().equals(user.getId())) {
                    text = "Demoted ";
                    text += MessageUtilities.mentionUser(entities, user, text.length()) + "!";
                    PromoteChatMember demote = PromoteChatMember.builder().chatId(message.getChatId().toString())
                            .userId(user.getId()).build();
                    try {
                        instance.execute(demote);
                    } catch (TelegramApiException ex) {
                        text = "Failed to demote ";
                        entities.clear();
                        text += MessageUtilities.mentionUser(entities, user, text.length()) + "!";
                    }
                    break;
                }
            }
        }
        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setEntities(entities);
        sm.setReplyToMessageId(message.getMessageId());
        instance.execute(sm);
    }
}
