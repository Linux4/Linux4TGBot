package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import org.checkerframework.checker.formatter.FormatUtil;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Command {

    public record Category(String name, String description){};
    public record HelpInfo(String parameters, String description){};

    public static final Category CATEGORY_ADMIN = new Category("Admin", "Make it easy to promote and demote users with the admin module\\!");
    public static final Category CATEGORY_BANS = new Category("Bans", "Some people need to be publicly banned; spammers," +
            " annoyances, or just trolls\\.\n\n" +
            "This module allows you to do that easily, by exposing some common actions, so everyone will see\\!");
    public static final Category CATEGORY_GREETINGS = new Category("Greetings", "Give your members a warm welcome with the greetings module\\!\n" +
            "Or a sad goodbye\\.\\.\\. Depends\\!");
    public static final Category CATEGORY_NOTES = new Category("Notes", "Save data for future users with notes\\!\n\n" +
            "Notes are great to save random tidbits of information; a phone number, a nice gif, a funny picture \\- anything\\!");
    public static final Category CATEGORY_MISC = new Category("Misc", "An \"odds and ends\" module for small," +
            " simple commands which don't really fit anywhere\\.");
    public static final Category CATEGORY_RULES = new Category("Rules", "Every chat works with different rules;" +
            " this module will help make those rules clearer\\!");
    public static final Category CATEGORY_INVISIBLE = new Category("NoDisplay", "Default (not visible in /help) category\\.");
    public static final List<Category> ALL_CATEGORIES = List.of(CATEGORY_ADMIN, CATEGORY_BANS, CATEGORY_GREETINGS,
            CATEGORY_NOTES, CATEGORY_MISC, CATEGORY_RULES);

    protected final Linux4Bot instance;
    private final List<String> commands;

    public Command(Linux4Bot instance, String... commands) {
        this.instance = instance;
        this.commands = Arrays.asList(commands);
    }

    public Category getCategory() {
        return CATEGORY_INVISIBLE;
    }

    public List<String> getCommands() {
        return commands;
    }

    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("", "No description available");
    }

    public boolean isUserCommand(String command) {
        return true;
    }

    public void callback(CallbackQuery query) throws TelegramApiException {};

    public abstract void execute(String command, Message message) throws TelegramApiException;

}
