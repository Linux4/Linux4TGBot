package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class DemoteCommand extends Command {

    public DemoteCommand(Linux4Bot instance) {
        super(instance, "demote");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            User user = instance.getUserRef(message);
            String text = "User required!";

            if (user != null) {
                text = "User isn't Admin in this group!";
                GetChatAdministrators administrators = GetChatAdministrators.builder()
                        .chatId(message.getChatId().toString()).build();
                for (ChatMember member : instance.execute(administrators)) {
                    if (member.getUser().getId().equals(user.getId())) {
                        text = "Demoted " + user.getUserName() + "!";
                        PromoteChatMember demote = PromoteChatMember.builder().chatId(message.getChatId().toString())
                                .userId(user.getId()).build();
                        try {
                            instance.execute(demote);
                        } catch (TelegramApiException ex) {
                            text = "Failed to demote " + user.getUserName() + "!";
                        }
                        break;
                    }
                }
            }
            SendMessage sm = new SendMessage(message.getChatId().toString(), text);
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
