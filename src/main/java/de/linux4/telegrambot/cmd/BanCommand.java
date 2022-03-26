package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.BanChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BanCommand extends Command {

    public BanCommand(Linux4Bot instance) {
        super(instance, "kick", "ban", "unban");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            boolean isKick = command.equalsIgnoreCase("kick");
            boolean isUnban = command.equalsIgnoreCase("unban");
            String text = "User required!";
            User user = instance.getUserRef(message);

            if (user != null) {
                text = "Cannot " + (isKick ? "kick" : "ban") + " Admins!";
                boolean admin = false;
                GetChatAdministrators admins = GetChatAdministrators.builder().chatId(message.getChatId().toString())
                        .build();
                for (ChatMember member : instance.execute(admins)) {
                    if (member.getUser().getId().equals(user.getId())) {
                        admin = true;
                        break;
                    }
                }

                if (!admin) {
                    try {
                        if (!isUnban) {
                            BanChatMember ban = BanChatMember.builder().chatId(message.getChatId().toString())
                                    .userId(user.getId()).untilDate(0).build();
                            instance.execute(ban);
                        }
                        if (isKick || isUnban) {
                            UnbanChatMember unban = UnbanChatMember.builder().chatId(message.getChatId().toString())
                                    .userId(user.getId()).build();
                            instance.execute(unban);
                        }
                        text = (isKick ? "Kicked" : (isUnban ? "Unbanned" : "Banned")) + " " + user.getUserName() + "!";
                    } catch (TelegramApiException ex) {
                        text = "Failed to " + (isKick ? "kick" : (isUnban ? "unban" : "ban")) + " " + user.getUserName() + "!";
                    }
                }
            }

            SendMessage sm = new SendMessage(message.getChatId().toString(), text);
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
