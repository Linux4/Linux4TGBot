package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Date;

public class TMuteCommand extends Command {

    public TMuteCommand(Linux4Bot instance) {
        super(instance, "tmute", "unmute");
    }

    private Integer parseDuration(String duration) {
        long time = 1;
        try {
            time = Integer.parseInt(duration.substring(0, Math.max(1, duration.length() - 1)));
            String timeUnit = "m";
            if (duration.length() > 1) timeUnit = duration.substring(1, 2);

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
                default: break;
            }
        } catch (NumberFormatException ignored) {

        }
        // min -> sec -> msec
        time *= 60 * 1000;

        Date date = new Date();
        date.setTime(date.getTime() + time);
        return (int)(date.getTime() / 1000);
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (instance.enforceChatAdmin(message)) {
            String text = "User required!";
            User user = instance.getUserRef(message);

            if (user != null) {
                if (command.equalsIgnoreCase("tmute")) {
                    text = "Cannot mute Admins!";
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
                        text = "Duration required!";
                        String[] broken = message.getText().trim().split(" ");
                        if ((message.getReplyToMessage() != null && broken.length > 1) || (broken.length > 2)) {
                            String duration = message.getReplyToMessage() != null ? broken[1] : broken[2];
                            Integer dateEnd = parseDuration(duration);
                            text = "Muted " + user.getUserName() + " until " + new Date((long)dateEnd * 1000) + "!";
                            RestrictChatMember restrict = RestrictChatMember.builder().userId(user.getId())
                                    .chatId(message.getChatId().toString())
                                    .permissions(ChatPermissions.builder().canSendMessages(false)
                                            .canSendMediaMessages(false).canSendOtherMessages(false).build())
                                    .untilDate(dateEnd).build();
                            instance.execute(restrict);
                        }
                    }
                } else { // unmute
                    text = "Unmuted " + user.getUserName() + "!";
                    RestrictChatMember restrict = RestrictChatMember.builder().userId(user.getId())
                            .chatId(message.getChatId().toString())
                            .permissions(ChatPermissions.builder().build()).build();
                    instance.execute(restrict);
                }
            }

            SendMessage sm = new SendMessage(message.getChatId().toString(), text);
            sm.setReplyToMessageId(message.getMessageId());
            instance.execute(sm);
        }
    }
}
