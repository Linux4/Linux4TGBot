package de.linux4.telegrambot;

import de.linux4.telegrambot.cmd.*;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static de.linux4.telegrambot.TelegramConstants.COMMAND_PREFIX;

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
    public Cron cron = new Cron();
    public final HashMap<Long, HashSet<Long>> captcha = new HashMap<>();
    public final GPT4All gpt4All;

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

        connect();

        Settings.init(this);
        UserUtilities.init(this);

        this.commands.add(new AskCommand(this));
        this.commands.add(new BanCommand(this));
        this.commands.add(new CaptchaCommand(this));
        this.commands.add(new DeleteCommand(this));
        this.commands.add(new DemoteCommand(this));
        this.commands.add(new FilterCommand(this));
        this.commands.add(new HelpCommand(this));
        this.commands.add(new IDCommand(this));
        this.commands.add(new NotesCommand(this));
        this.commands.add(new PromoteCommand(this));
        this.commands.add(new RulesCommand(this));
        this.commands.add(new SedCommand(this));
        this.commands.add(new SetRulesCommand(this));
        this.commands.add(new SetWelcomeCommand(this));
        this.commands.add(new StartCommand(this));
        this.commands.add(new TMuteCommand(this));

        cron.start();

        gpt4All = new GPT4All(this, Path.of(".", "gpt4all-model.gguf"));
        gpt4All.start();
    }

    private void connect() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            mysql = DriverManager.getConnection("jdbc:mariadb://10.2.0.1:3306/linux4tgbot?autoReconnect=true&useUnicode=true"
                            + "&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC",
                    "linux4", "linux4!");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
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

        try {
            if (!mysql.isValid(3000)) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            connect();
        }

        // Ban premium stickers
        if (update.hasMessage() && update.getMessage().hasSticker() && update.getMessage().getSticker().getPremiumAnimation() != null) {
            DeleteMessage dm = DeleteMessage.builder().chatId(update.getMessage().getChatId()).messageId(update.getMessage().getMessageId())
                    .build();
            try {
                execute(dm);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

        // Check if this is an allowed group
        try {
            if (update.hasMyChatMember() && !update.getMyChatMember().getChat().isUserChat()) {
                boolean allowed = false;
                GetChatAdministrators administrators = GetChatAdministrators.builder()
                        .chatId(update.getMyChatMember().getChat().getId().toString()).build();
                for (ChatMember admin : execute(administrators)) {
                    if (getMe().getUserName()
                            .substring(0, getMe().getUserName().length() - "bot".length()).equalsIgnoreCase(admin.getUser().getUserName())) {
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

        if (update.hasMessage() && (update.getMessage().getNewChatMembers() != null || update.getMessage().getLeftChatMember() != null)) {
            boolean join = update.getMessage().getLeftChatMember() == null;
            List<User> users = new ArrayList<>();
            if (join)
                users.addAll(update.getMessage().getNewChatMembers());
            else
                users.add(update.getMessage().getLeftChatMember());

            for (User user : users) {
                UserUtilities.setUserName(this, user.getId(), user.getUserName());

                try {
                    if (user.getId().equals(this.getMe().getId()))
                        continue;
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }

                String welcomeMsg = SetWelcomeCommand.getWelcomeMessage(this,
                        update.getMessage().getChatId(), join);
                boolean captcha = Settings.getSettingBool(this, update.getMessage().getChatId(),
                        Settings.KEY_CAPTCHA, false);

                if (welcomeMsg != null || (join && captcha)) {
                    int index = 0;
                    if (welcomeMsg == null) welcomeMsg = "CAPTCHA";
                    List<MessageEntity> entities = new ArrayList<>();
                    String userName = user.getUserName();
                    if (userName == null) {
                        userName = user.getFirstName();
                        if (user.getLastName() != null) {
                            userName += " " + user.getLastName();
                        }
                    }
                    for (index = welcomeMsg.indexOf("{username}"); index >= 0; index = welcomeMsg.indexOf("{username}", index + 1)) {
                        MessageEntity entity = MessageEntity.builder().type(EntityType.TEXTMENTION)
                                .user(user)
                                .offset(index + (entities.size() * userName.length() + (entities.size() > 0 ? 1 : 0))
                                        - (entities.size() * "{username}".length()))
                                .length(userName.length()).build();
                        entities.add(entity);
                    }
                    welcomeMsg = welcomeMsg.replaceAll("\\{username}", userName);
                    welcomeMsg = welcomeMsg.replaceAll("\\{chatname}", update.getMessage().getChat().getTitle());
                    welcomeMsg = welcomeMsg.replaceAll("\\{id}", user.getId().toString());

                    InlineKeyboardMarkup captchaKb = null;
                    if (captcha) {
                        if (!this.captcha.containsKey(update.getMessage().getChatId()))
                            this.captcha.put(update.getMessage().getChatId(), new HashSet<>());
                        this.captcha.get(update.getMessage().getChatId()).add(user.getId());
                        cron.tasks.add(new CronTask(System.currentTimeMillis() + 5 * 60 * 1000, () -> {
                            if (Linux4Bot.this.captcha.containsKey(update.getMessage().getChatId()) &&
                                    Linux4Bot.this.captcha.get(update.getMessage().getChatId()).contains(user.getId())) {
                                // Kick the user for inactivity after 5 minutes
                                try {
                                    BanChatMember ban = BanChatMember.builder().chatId(update.getMessage().getChatId().toString())
                                            .userId(user.getId()).build();
                                    Linux4Bot.this.execute(ban);
                                    UnbanChatMember unban = UnbanChatMember.builder().chatId(ban.getChatId()).userId(ban.getUserId()).build();
                                    Linux4Bot.this.execute(unban);
                                } catch (TelegramApiException e) {
                                    e.printStackTrace();
                                }
                                Linux4Bot.this.captcha.get(update.getMessage().getChatId()).remove(user.getId());
                                if (Linux4Bot.this.captcha.get(update.getMessage().getChatId()).size() == 0)
                                    Linux4Bot.this.captcha.remove(update.getMessage().getChatId());
                            }
                        }));

                        captchaKb = InlineKeyboardMarkup.builder().keyboardRow(
                                List.of(InlineKeyboardButton.builder().callbackData("captcha_" + user.getId())
                                        .text("Click here to prove you're human").build())
                        ).build();
                    }

                    SendMessage sm = new SendMessage(update.getMessage().getChatId().toString(), welcomeMsg);
                    sm.setEntities(entities);
                    sm.setReplyMarkup(captchaKb);
                    try {
                        execute(sm);
                    } catch (TelegramApiException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (update.hasMessage()) {
            UserUtilities.setUserName(this, update.getMessage().getFrom().getId(),
                    update.getMessage().getFrom().getUserName());

            // Captcha check
            if (captcha.containsKey(update.getMessage().getChatId()) &&
                    captcha.get(update.getMessage().getChatId()).contains(update.getMessage().getFrom().getId())) {
                // Delete message
                DeleteMessage dm = DeleteMessage.builder().messageId(update.getMessage().getMessageId())
                        .chatId(String.valueOf(update.getMessage().getChatId())).build();
                try {
                    execute(dm);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }

        if (update.hasCallbackQuery()) {
            String commandName = update.getCallbackQuery().getData().split("_")[0];
            for (Command command : commands) {
                for (String otherCommandName : command.getCommands()) {
                    if (commandName.equalsIgnoreCase(otherCommandName)) {
                        try {
                            command.callback(update.getCallbackQuery());
                            execute(AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId()).build());
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            String firstWord = update.getMessage().getText().trim().split(" ")[0];
            // Only allow help and rules in pm
            if (update.getMessage().isUserMessage() && !firstWord.equalsIgnoreCase(COMMAND_PREFIX +"help")
                    && !firstWord.equalsIgnoreCase(COMMAND_PREFIX + "start") && !firstWord.equalsIgnoreCase(COMMAND_PREFIX + "id"))
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
            } else if (firstWord.startsWith("s/") || firstWord.startsWith("'s/")) {
                // Sed
                for (Command command : commands) {
                    if (command.getCommands().contains("sed")) {
                        try {
                            command.execute("sed", update.getMessage());
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (firstWord.startsWith(COMMAND_PREFIX)) {
                for (Command command : commands) {
                    for (String commandName : command.getCommands()) {
                        if (commandName.equalsIgnoreCase(firstWord.substring(1))) {
                            try {
                                if (command.isUserCommand(commandName) || enforceChatAdmin(update.getMessage())) {
                                    command.execute(commandName, update.getMessage());
                                }
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            } else { // Filters
                FilterCommand filters = null;
                for (Command command : commands) {
                    if (command.getCommands().contains("filter")) {
                        filters = (FilterCommand) command;
                        break;
                    }
                }

                if (filters != null) {
                    for (String filterName : filters.getMatchingFilters(update.getMessage().getChatId(), update.getMessage().getText(), false)) {
                        String filterAction = filters.getFilterAction(update.getMessage().getChatId(), filterName);
                        SendMessage sm = new SendMessage(update.getMessage().getChatId().toString(), filterAction);
                        sm.setReplyToMessageId(update.getMessage().getMessageId());
                        try {
                            Message reply = execute(sm);

                            if (filterAction.startsWith(COMMAND_PREFIX)) {
                                // is a command
                                String command = filterAction.split(" ")[0].substring(1);

                                for (Command execute : commands) {
                                    for (String executeCmd : execute.getCommands()) {
                                        if (command.equalsIgnoreCase(executeCmd)) {
                                            execute.execute(executeCmd, reply);
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
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
