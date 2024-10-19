package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TMuteCommand extends Command {

    public TMuteCommand(Linux4Bot instance) {
        super(instance, "tmute", "unmute");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_BANS;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        switch (command) {
            case "tmute":
                return new HelpInfo("", "Temporarily mute a user\\." +
                        "Example time values: 4m \\= 4 minutes, 3h \\= 3 hours, 6d \\= 6 days, 5w \\= 5 weeks\\.");
            case "unmute":
                return new HelpInfo("", "Unmute a user\\.");
        }
        return super.getHelpInfo(command);
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    private static Integer parseDuration(String duration) {
        long time = 1;
        try {
            time = Integer.parseInt(duration.substring(0, Math.max(1, duration.length() - 1)));
            String timeUnit = "m";
            if (duration.length() > 1) timeUnit = duration.substring(duration.length() - 1);

            switch (timeUnit) {
                case "Y":
                    time *= 12;
                case "M":
                    time *= 31;
                case "D":
                case "d":
                    time *= 24;
                case "h":
                    time *= 60;
                default:
                    break;
            }
        } catch (NumberFormatException ignored) {

        }
        // min -> sec -> msec
        time *= 60 * 1000;

        Date date = new Date();
        date.setTime(date.getTime() + time);
        return (int) (date.getTime() / 1000);
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text = "User required!";
        List<MessageEntity> entities = new ArrayList<>();
        User user = instance.getUserRef(message);

        if (user != null) {
            if (command.equalsIgnoreCase("tmute")) {
                text = "Cannot mute Admins!";
                boolean admin = false;
                GetChatAdministrators admins = GetChatAdministrators.builder().chatId(message.getChatId().toString())
                        .build();
                for (ChatMember member : instance.telegramClient.execute(admins)) {
                    if (member.getUser().getId().equals(user.getId())) {
                        admin = true;
                        break;
                    }
                }

                if (!admin) {
                    text = "Duration required!";
                    String[] broken = message.getText().trim().split(" ");
                    if ((message.getReplyToMessage() != null && broken.length > 1) || (broken.length > 2)) {
                        String duration = message.getReplyToMessage() != null ? broken[1] : broken[2];
                        Integer dateEnd = parseDuration(duration);
                        text = "Muted ";
                        text += MessageUtilities.mentionUser(entities, user, text.length()) + " until " + new Date((long) dateEnd * 1000) + "!";
                        RestrictChatMember restrict = RestrictChatMember.builder().userId(user.getId())
                                .chatId(message.getChatId().toString())
                                .permissions(ChatPermissions.builder()
                                        .canSendMessages(false)
                                        .canSendAudios(false)
                                        .canSendPolls(false)
                                        .canSendDocuments(false)
                                        .canSendPhotos(false)
                                        .canSendVideos(false)
                                        .canSendVideoNotes(false)
                                        .canSendVoiceNotes(false)
                                        .canSendOtherMessages(false).build())
                                .untilDate(dateEnd).build();
                        instance.telegramClient.execute(restrict);
                    }
                }
            } else { // unmute
                text = "Unmuted ";
                text += MessageUtilities.mentionUser(entities, user, text.length()) + "!";
                RestrictChatMember restrict = RestrictChatMember.builder().userId(user.getId())
                        .chatId(message.getChatId().toString())
                        .permissions(ChatPermissions.builder().canSendMessages(true)
                                .canAddWebPagePreviews(true)
                                .canSendAudios(true)
                                .canSendPolls(true)
                                .canSendDocuments(true)
                                .canSendPhotos(true)
                                .canSendVideos(true)
                                .canSendVideoNotes(true)
                                .canSendVoiceNotes(true)
                                .canSendOtherMessages(true)
                                .canInviteUsers(true)
                                .canPinMessages(true)
                                .canChangeInfo(true)
                                .canSendPolls(true).build()).build();
                instance.telegramClient.execute(restrict);
            }
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setEntities(entities);
        sm.setReplyToMessageId(message.getMessageId());
        instance.telegramClient.execute(sm);
    }
}
