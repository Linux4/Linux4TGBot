package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterCommand extends Command {

    public FilterCommand(Linux4Bot instance) {
        super(instance, "filter", "filters", "stop");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Filters (ChatID LONG, Filter varchar(4096), Reply varchar(4096))")
                    .executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_FILTERS;
    }

    public String getFilterAction(long chatId, String filter) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("SELECT Reply FROM Filters WHERE ChatID = " + chatId
                + " AND Filter = ?");
            ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("Reply");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<String> getMatchingFilters(long chatId, String text, boolean all) {
        List<String> filters = new ArrayList<>();

        try {
            PreparedStatement ps = instance.mysql.prepareStatement("SELECT Filter FROM Filters WHERE ChatID = " + chatId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Pattern pattern = Pattern.compile(rs.getString("Filter"));
                Matcher matcher = pattern.matcher(text);

                if (all || matcher.find()) {
                    filters.add(rs.getString("Filter"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return filters;
    }

    private void saveFilter(long chatId, String filter, String reply) {
        try {
            PreparedStatement ps;
            if (getFilterAction(chatId, filter) == null) {
                ps = instance.mysql.prepareStatement("INSERT INTO Filters (Reply, ChatID, Filter) VALUES (?, " + chatId + ", ?)");
            } else {
                ps = instance.mysql.prepareStatement("UPDATE Filters SET Reply = ? WHERE ChatID = " + chatId + " AND Filter = ?");
            }
            ps.setString(1, reply);
            ps.setString(2, filter);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteFilter(long chatId, String filter) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("DELETE FROM Filters WHERE ChatID = " + chatId + " AND Filter = ?");
            ps.setString(1, filter);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        switch (command) {
            case "filter":
                return new HelpInfo("<trigger> <reply>", "Every time someone says \"trigger\", the bot will reply with \"reply\"\\. " +
                        "For multiple word filters, quote the trigger\\. " +
                        "\"reply\" can also be a command to run\\.");
            case "filters":
                return new HelpInfo("", "List all chat filters\\.");
            case "stop":
                return new HelpInfo("<trigger>", "Stop the bot from responding to \"trigger\"\\.");
        }

        return super.getHelpInfo(command);
    }

    @Override
    public boolean isUserCommand(String command) {
        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text = "";
        String[] broken = message.getText().trim().split(" ");

        switch (command) {
            case "filter":
                text = "Filter Name required!";
                if (broken.length > 1) {
                    String filterName;
                    String filterAction = null;
                    if (broken[1].startsWith("\"")) {
                        filterName = message.getText().split("\"")[1];
                        int endIndex = message.getText().indexOf(filterName) + filterName.length() + 1;

                        if (message.getText().length() > endIndex + 1) {
                            filterAction = message.getText().substring(endIndex + 1);
                        }
                    } else {
                        filterName = broken[1];
                        if (broken.length > 2) {
                            filterAction = Joiner.on(' ').join(Arrays.copyOfRange(broken, 2, broken.length));
                        }
                    }

                    if (filterAction != null) {
                        saveFilter(message.getChatId(), filterName, filterAction);
                        text = "Saved filter " + filterName;
                    } else {
                        // Print action
                        text = "Filter not found!";
                        filterAction = getFilterAction(message.getChatId(), filterName);
                        if (filterAction != null)
                            text = "Action for " + filterName + ": " + filterAction;
                    }
                }
                break;
            case "filters":
                text = "List of filters in " + message.getChat().getTitle() + ":\n";
                for (String filterName : getMatchingFilters(message.getChatId(), "", true)) {
                    text += "- <code>" + filterName + "</code>\n";
                }
                break;
            case "stop":
                String filterName = Joiner.on(' ').join(Arrays.copyOfRange(broken, 1, broken.length));
                text = "Filter not found!";
                if (getFilterAction(message.getChatId(), filterName) != null) {
                    text = "Filter " + filterName + " removed!";
                    deleteFilter(message.getChatId(), filterName);
                }
                break;
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        sm.setParseMode("HTML");
        sm.setReplyToMessageId(message.getMessageId());
        instance.execute(sm);
    }
}
