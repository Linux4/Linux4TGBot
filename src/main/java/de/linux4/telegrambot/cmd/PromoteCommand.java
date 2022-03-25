package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatAdministratorCustomTitle;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;

public class PromoteCommand extends Command {

    public PromoteCommand(Linux4Bot instance) {
        super(instance, "promote");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            User user = instance.getUserRef(message);
            String text = "User required!";
            String rank = "";
            String[] broken = message.getText().trim().split(" ");

            if (broken.length > 2 && broken[1].startsWith("@")) {
                rank = Joiner.on(' ').join(Arrays.copyOfRange(broken, 2, broken.length));
            } else if (message.getText().trim().split(" ").length > 1) {
                rank = Joiner.on(' ').join(Arrays.copyOfRange(broken, 1, broken.length));
            }

            if (user != null) {
                PromoteChatMember promote = PromoteChatMember.builder().chatId(message.getChatId().toString())
                        .userId(user.getId()).canChangeInformation(true).canDeleteMessages(true)
                        .canRestrictMembers(true).canInviteUsers(true).canPinMessages(true).canManageVoiceChats(true).build();
                text = "Promoted " + user.getUserName() + "!";
                try {
                    if (instance.execute(promote)) {

                        if (rank.length() > 0) {
                            try {
                                Thread.sleep(100); // Give it some time to update user state
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            SetChatAdministratorCustomTitle title = SetChatAdministratorCustomTitle.builder()
                                    .chatId(message.getChatId().toString())
                                    .userId(user.getId()).customTitle(rank).build();
                            instance.execute(title);
                        }
                    }
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                    text = "Failed to promote " + user.getUserName() + "!";
                }
            }
            SendMessage sm = new SendMessage(message.getChatId().toString(), text);
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
