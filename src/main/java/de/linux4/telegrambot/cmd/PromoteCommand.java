package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatAdministratorCustomTitle;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PromoteCommand extends Command {

    public PromoteCommand(Linux4Bot instance) {
        super(instance, "promote");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_ADMIN;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("<reply\\/username\\/mention\\/userid>", "Promote a user\\.");
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
        String rank = "";
        String[] broken = message.getText().trim().split(" ");

        if (broken.length >= 2 && broken[1].startsWith("@")) {
            rank = Joiner.on(' ').join(Arrays.copyOfRange(broken, 2, broken.length));
        } else if (message.getText().trim().split(" ").length > 1) {
            rank = Joiner.on(' ').join(Arrays.copyOfRange(broken, 1, broken.length));
        }

        if (user != null) {
            PromoteChatMember promote = PromoteChatMember.builder().chatId(message.getChatId().toString())
                    .userId(user.getId()).canChangeInformation(true).canDeleteMessages(true)
                    .canRestrictMembers(true).canInviteUsers(true).canPinMessages(true).canManageVoiceChats(true).build();
            text = "Promoted ";
            text += MessageUtilities.mentionUser(entities, user, text.length()) + "!";
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
                entities.clear();
                text = "Failed to promote ";
                text += MessageUtilities.mentionUser(entities, user, text.length()) + "!";
            }
        }
        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setEntities(entities);
        sm.setReplyToMessageId(message.getMessageId());
        instance.execute(sm);
    }
}
