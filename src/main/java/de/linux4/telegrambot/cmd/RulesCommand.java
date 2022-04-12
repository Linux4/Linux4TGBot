package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.EntityType;
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
                    String preText1 = "The rules for ";
                    String preText2 = chatTitle;
                    String preText3 = " are:\n\n";
                    String text = "This chat doesn't seem to have had any rules set yet..."
                            + "I wouldn't take that as an invitation though.";
                    List<MessageEntity> entities = new ArrayList<>();
                    entities.add(MessageEntity.builder().type(EntityType.BOLD).offset(0)
                            .length(preText1.length()).build());
                    entities.add(MessageEntity.builder().type(EntityType.CODE).offset(preText1.length())
                            .length(preText2.length()).build());
                    entities.add(MessageEntity.builder().type(EntityType.BOLD)
                            .offset(preText1.length() + preText2.length()).length(preText3.length()).build());
                    try {
                        ResultSet rs = instance.mysql.prepareStatement("SELECT Text, Entities FROM Rules WHERE ChatID = " + chatId)
                                .executeQuery();
                        if (rs.next()) {
                            text = rs.getString("Text");
                            entities.addAll(MessageUtilities.entitiesFromString(rs.getString("Entities"),
                                    preText1.length() + preText2.length() + preText3.length()));
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    SendMessage sm = new SendMessage(message.getChatId().toString(), preText1 + preText2 + preText3 + text);
                    sm.setReplyToMessageId(message.getMessageId());
                    sm.setEntities(entities);
                    instance.execute(sm);
                } catch (NumberFormatException ignored) {

                }
            }
        }
    }
}
