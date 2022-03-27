package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RulesCommand extends Command {

    public RulesCommand(Linux4Bot instance) {
        super(instance, "rules");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_RULES;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("", "Check the current chat rules\\.");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        if (command.equalsIgnoreCase("rules")) {
            SendMessage sm = new SendMessage(message.getChatId().toString(), "Click on the button to see the chat rules!");
            sm.setReplyToMessageId(message.getMessageId());

            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder().keyboardRow(
                    List.of(InlineKeyboardButton.builder().text("Rules")
                            .url("https://t.me/" + instance.getMe().getUserName() + "?start=rules_" + message.getChatId())
                            .build()));
            sm.setReplyMarkup(keyboardBuilder.build());

            instance.execute(sm);
        } else { // Called from /start as rules_<chatid>
            if (command.split("_").length > 1) {
                try {
                    Long chatId = Long.parseLong(command.split("_")[1]);
                    String chatTitle = instance.execute(GetChat.builder().chatId(chatId.toString()).build()).getTitle();
                    String text = "No rules set for Chat " + chatTitle + "!";
                    List<MessageEntity> entities = new ArrayList<>();
                    try {
                        ResultSet rs = instance.mysql.prepareStatement("SELECT Text, Entities FROM Rules WHERE ChatID = " + chatId)
                                .executeQuery();
                        if (rs.next()) {
                            text = rs.getString("Text");
                            entities = MessageUtilities.entitiesFromString(rs.getString("Entities"));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    SendMessage sm = new SendMessage(message.getChatId().toString(), text);
                    sm.setReplyToMessageId(message.getMessageId());
                    sm.setEntities(entities);
                    instance.execute(sm);
                } catch (NumberFormatException ignored) {

                }
            }
        }
    }
}
