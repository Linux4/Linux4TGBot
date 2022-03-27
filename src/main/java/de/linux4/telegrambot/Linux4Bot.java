package de.linux4.telegrambot;

import de.linux4.telegrambot.cmd.*;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.LeaveChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
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
    public final List<Command> commands = new ArrayList<>();
    public Connection mysql;

    public Linux4Bot(String botToken) {
        super(new DefaultBotOptions() {
            @Override
            public List<String> getAllowedUpdates() {
                // All updates
                return List.of("message", "edited_message", "channel_post", "edited_channel_post", "inline_query",
                        "chosen_inline_result", "callback_query", "shipping_query", "pre_checkout_query", "poll",
                        "poll_answer", "my_chat_member", "chat_member", "chat_join_request");
            }
        });
        this.botToken = botToken;

        try {
            mysql = DriverManager.getConnection("jdbc:mysql://localhost:3306/linux4tgbot?autoReconnect=true&useUnicode=true"
                            + "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
                    "linux4", "linux4!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        UserUtilities.init(this);

        this.commands.add(new BanCommand(this));
        this.commands.add(new DemoteCommand(this));
        this.commands.add(new IDCommand(this));
        this.commands.add(new NotesCommand(this));
        this.commands.add(new PromoteCommand(this));
        this.commands.add(new RulesCommand(this));
        this.commands.add(new SetRulesCommand(this));
        this.commands.add(new SetWelcomeCommand(this));
        this.commands.add(new StartCommand(this));
        this.commands.add(new TMuteCommand(this));
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

        // Check if this is an allowed group
        try {
            if (update.hasMyChatMember() && !update.getMyChatMember().getChat().isUserChat()) {
                boolean allowed = false;
                GetChatAdministrators administrators = GetChatAdministrators.builder()
                        .chatId(update.getMyChatMember().getChat().getId().toString()).build();
                for (ChatMember admin : execute(administrators)) {
                    if (admin.getUser().getUserName().equalsIgnoreCase(getMe().getUserName()
                            .substring(0, getMe().getUserName().length() - "bot".length()))) {
                        allowed = true;
                        break;
                    }
                }

                if (!allowed) {
                    SendMessage sm = new SendMessage(update.getMyChatMember().getChat().getId().toString(),
                            "This group is not authorized to use the bot!");
                    execute(sm);
                    execute(LeaveChat.builder().chatId(update.getMyChatMember().getChat().getId().toString()).build());
                }
            }
        } catch (TelegramApiException ignored) {
        }

        if (update.hasChatMember()) {
            UserUtilities.setUserName(this, update.getChatMember().getNewChatMember().getUser().getId(),
                    update.getChatMember().getNewChatMember().getUser().getUserName());

            String status = update.getChatMember().getNewChatMember().getStatus();
            if (status.equalsIgnoreCase("member") || status.equalsIgnoreCase("left")) {
                String welcomeMsg = SetWelcomeCommand.getWelcomeMessage(this,
                        update.getChatMember().getChat().getId(), status.equalsIgnoreCase("member"));

                if (welcomeMsg != null) {
                    int index = 0;
                    List<MessageEntity> entities = new ArrayList<>();
                    String userName = update.getChatMember().getNewChatMember().getUser().getUserName();
                    if (userName == null) userName = update.getChatMember().getNewChatMember().getUser().getFirstName();
                    for (index = welcomeMsg.indexOf("{username}"); index >= 0; index = welcomeMsg.indexOf("{username}", index + 1)) {
                        MessageEntity entity = MessageEntity.builder().type(EntityType.TEXTMENTION)
                                .user(update.getChatMember().getNewChatMember().getUser())
                                .offset(index + (entities.size() * userName.length() + (entities.size() > 0 ? 1 : 0))
                                        - (entities.size() * "{username}".length()))
                                .length(userName.length()).build();
                        entities.add(entity);
                    }
                    welcomeMsg = welcomeMsg.replaceAll("\\{username}", userName);
                    welcomeMsg = welcomeMsg.replaceAll("\\{chatname}", update.getChatMember().getChat().getTitle());
                    SendMessage sm = new SendMessage(update.getChatMember().getChat().getId().toString(), welcomeMsg);
                    sm.setEntities(entities);
                    try {
                        execute(sm);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (update.hasMessage()) {
            UserUtilities.setUserName(this, update.getMessage().getFrom().getId(),
                    update.getMessage().getFrom().getUserName());
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String firstWord = update.getMessage().getText().trim().split(" ")[0];
            // Only allow help and rules in pm
            if (update.getMessage().isUserMessage() && !firstWord.equalsIgnoreCase("/help")
                    && !firstWord.equalsIgnoreCase("/start") && !firstWord.equalsIgnoreCase("/id"))
                return;

            try {
                if (firstWord.contains("@") && firstWord.split("@")[1].equalsIgnoreCase(getMe().getUserName())) {
                    firstWord = firstWord.split("@")[0];
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }

            if (firstWord.startsWith("#")) {
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
            } else if (firstWord.startsWith("/")) {
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

    public User getUserRef(Message message) throws TelegramApiException {
        if (message.getReplyToMessage() != null) {
            return message.getReplyToMessage().getFrom();
        }

        if (message.getEntities() != null) {
            for (MessageEntity entity : message.getEntities()) {
                if (entity.getType().equals(EntityType.MENTION)) {
                    Long userId = UserUtilities.getUserId(this, entity.getText().substring(1));
                    if (userId != null) {
                        GetChatMember member = GetChatMember.builder().chatId(message.getChatId().toString())
                                .userId(userId).build();
                        User user = execute(member).getUser();

                        if (user != null) {
                            UserUtilities.setUserName(this, user.getId(), user.getUserName());
                            return user;
                        }
                    }

                    SendMessage sm = new SendMessage(message.getChatId().toString(), "Unknown user!");
                    sm.setReplyToMessageId(message.getMessageId());
                    execute(sm);
                } else if (entity.getType().equals(EntityType.TEXTMENTION)) {
                    return entity.getUser();
                }
            }
        }

        return null;
    }
}
