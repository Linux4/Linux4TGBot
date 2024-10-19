package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HelpCommand extends Command {

    public HelpCommand(Linux4Bot instance) {
        super(instance, "help");
    }

    @Override
    public void callback(CallbackQuery query) throws TelegramApiException {
        String category = query.getData().split("_").length > 1 ? query.getData().split("_")[1] : "";
        if (category.length() > 0) {
            Category categoryObj = null;
            for (Category cat : Command.ALL_CATEGORIES) if (cat.name().equalsIgnoreCase(category)) categoryObj = cat;

            String text = "*" + categoryObj.name() + "*\n\n";
            text += categoryObj.description() + "\n\n";

            HashMap<String, Command> userCommands = new HashMap<>();
            HashMap<String, Command> adminCommands = new HashMap<>();
            for (Command command : instance.commands) {
                if (command.getCategory().name().equals(categoryObj.name())) {
                    for (String commandName : command.getCommands()) {
                        if (command.isUserCommand(commandName)) {
                            userCommands.put(commandName, command);
                        } else {
                            adminCommands.put(commandName, command);
                        }
                    }
                }
            }

            if (userCommands.keySet().size() > 0) {
                text += "*User commands:*\n";

                for (String command : userCommands.keySet()) {
                    HelpInfo helpInfo = userCommands.get(command).getHelpInfo(command);
                    text += "\\- /" + command
                            + (helpInfo.parameters() != null && helpInfo.parameters().length() > 0
                            ? " `" + helpInfo.parameters() + "`" : "") + ": " + helpInfo.description() + "\n";
                }
                if (adminCommands.keySet().size() > 0) text += "\n";
            }
            if (adminCommands.keySet().size() > 0) {
                text += "*Admin commands:*\n";

                for (String command : adminCommands.keySet()) {
                    HelpInfo helpInfo = adminCommands.get(command).getHelpInfo(command);
                    text += "\\- /" + command
                            + (helpInfo.parameters() != null && helpInfo.parameters().length() > 0
                            ? " `" + helpInfo.parameters() + "`" : "") + ": " + helpInfo.description() + "\n";
                }
            }

            InlineKeyboardMarkup back = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                            .text("Back").callbackData("help_").build()))
                    .build();
            instance.telegramClient.execute(EditMessageText.builder().chatId(query.getMessage().getChatId().toString())
                    .messageId(query.getMessage().getMessageId()).text(text)
                    .replyMarkup(back).parseMode("MARKDOWNV2").build());
        } else { // Back
            if (query.getMessage() instanceof Message) {
                execute("callback", (Message) query.getMessage());
            }
        }
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (message.getChat().isUserChat()) {
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            int row = 0;
            for (Category category : Command.ALL_CATEGORIES) {
                List<InlineKeyboardButton> buttons = row == 0 ? new ArrayList<>() : rows.get(rows.size() - 1);
                buttons.add(InlineKeyboardButton.builder().text(category.name())
                        .callbackData("help_" + category.name().toLowerCase())
                        .build());
                if (row == 0)
                    rows.add(buttons);
                row++;
                row = row % 3;
            }
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> keyboardBuilder = InlineKeyboardMarkup.builder();
            for (List<InlineKeyboardButton> buttons : rows) {
                keyboardBuilder.keyboardRow(new InlineKeyboardRow(buttons));
            }

            String text = "*Help*\n\n";
            text += "Hey\\! My name is `" + instance.getMe().getFirstName() + "`\\. I am a group management bot, ";
            text += "here to help you get around and keep the order in your groups\\!\n\n";
            text += "*Helpful commands*\n";
            text += "\\- /start: Starts me\\! You've probably already used this\\.\n";
            text += "\\- /help: Sends this message; I'll tell you more about myself\\!\n";
            if (command.equalsIgnoreCase("callback")) {
                EditMessageText em = EditMessageText.builder().chatId(message.getChatId().toString()).text(text)
                        .messageId(message.getMessageId())
                        .replyMarkup(keyboardBuilder.build())
                        .parseMode("MARKDOWNV2").build();
                instance.telegramClient.execute(em);
            } else {
                SendMessage sm = new SendMessage(message.getChatId().toString(), text);
                sm.setReplyToMessageId(message.getMessageId());
                sm.setReplyMarkup(keyboardBuilder.build());
                sm.setParseMode("MARKDOWNV2");
                instance.telegramClient.execute(sm);
            }
        } else {
            SendMessage sm = new SendMessage(message.getChatId().toString(), "Contact me in PM for help!");
            sm.setReplyToMessageId(message.getMessageId());

            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder().keyboardRow(
                    new InlineKeyboardRow(InlineKeyboardButton.builder().text("Click me for help!")
                            .url("https://t.me/" + instance.getMe().getUserName() + "?start=help_")
                            .build()));
            sm.setReplyMarkup(keyboardBuilder.build());

            instance.telegramClient.execute(sm);
        }
    }
}
