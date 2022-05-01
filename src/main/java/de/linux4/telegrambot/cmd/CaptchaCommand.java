package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.Settings;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatPermissions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class CaptchaCommand extends Command {

    public CaptchaCommand(Linux4Bot instance) {
        super(instance, "captcha");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_CAPTCHA;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("<yes/no/on/off>", "All users that join will need to solve a CAPTCHA\\. " +
                "This proves they aren't a bot\\!");
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text = "";
        String[] broken = message.getText().split(" ");
        if (broken.length > 1) {
            boolean value = broken[1].equalsIgnoreCase("on") || broken[1].equalsIgnoreCase("yes")
                    || broken[1].equalsIgnoreCase("y");
            Settings.saveSettingBool(instance, message.getChatId(), Settings.KEY_CAPTCHA, value);
            text = value ? "CAPTCHAs have been enabled. I will now mute people when they join."
                    : "CAPTCHAs have been disabled. Users can join normally.";
        } else {
            text = Settings.getSettingBool(instance, message.getChatId(), Settings.KEY_CAPTCHA, false) ?
                    "Users will be asked to complete a CAPTCHA before being allowed to speak in the chat.\n\n"
                    : "Users will NOT be muted when joining the chat.\n\n";
            text += "To change this setting, try this command again followed by one of yes/no/on/off";
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setReplyToMessageId(message.getMessageId());
        instance.execute(sm);
    }

    @Override
    public void callback(CallbackQuery query) throws TelegramApiException {
        long userId = Long.parseLong(query.getData().split("_")[1]);

        if (userId == query.getFrom().getId()) {
            instance.execute(EditMessageText.builder().chatId(query.getMessage().getChatId().toString())
                    .messageId(query.getMessage().getMessageId())
                    .text(query.getMessage().getText()).entities(query.getMessage().getEntities())
                    .replyMarkup(null).build());
            instance.captcha.get(query.getMessage().getChatId()).remove(userId);
            if (instance.captcha.get(query.getMessage().getChatId()).size() == 0)
                instance.captcha.remove(query.getMessage().getChatId());
        }
    }
}
