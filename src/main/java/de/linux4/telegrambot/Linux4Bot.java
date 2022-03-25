package de.linux4.telegrambot;

import de.linux4.telegrambot.cmd.Command;
import de.linux4.telegrambot.cmd.NotesCommand;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Linux4Bot extends TelegramLongPollingBot {

    public static void main(String[] args) {
        if (args.length == 1) {
            try {
                TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
                botApi.registerBot(new Linux4Bot(args[0]));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Usage: java -jar Linux4TGBot.jar <bot token>");
        }
    }

    private final String botToken;
    private final List<Command> commands = new ArrayList<>();
    public Connection mysql;

    public Linux4Bot(String botToken) {
        this.botToken = botToken;

        try {
            mysql = DriverManager.getConnection("jdbc:mysql://localhost:3306/linux4tgbot?autoReconnect=true&useUnicode=true"
                            + "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
                    "linux4", "linux4!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        this.commands.add(new NotesCommand(this));
    }

    @Override
    public String getBotUsername() {
        return "Linux4Bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (mysql == null) {
            System.err.println("Database not connected!");
            return;
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String firstWord = update.getMessage().getText().trim().split(" ")[0];

            if (update.getMessage().getText().startsWith("#")) {
                // is a Note
                for (Command command : commands) {
                    if (command.getCommands().contains("notes")) {
                        try {
                            command.execute("get", update.getMessage());
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            } else if (update.getMessage().getText().trim().startsWith("/")) {
                for (Command command : commands) {
                    for (String commandName : command.getCommands()) {
                        if (commandName.equalsIgnoreCase(firstWord.substring(1))) {
                            try {
                                command.execute(commandName, update.getMessage());
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean enforceChatAdmin(Message message) throws TelegramApiException {
        GetChatAdministrators admins = GetChatAdministrators.builder().chatId(message.getChatId().toString()).build();
        for (ChatMember member : execute(admins)) {
            if (member.getUser().getId().equals(message.getFrom().getId())) {
                return true;
            }
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), "You're not Admin in this group!");
        sm.setReplyToMessageId(message.getMessageId());
        execute(sm);
        return false;
    }
}