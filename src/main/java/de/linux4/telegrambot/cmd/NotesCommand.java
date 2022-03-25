package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NotesCommand extends Command {

    public NotesCommand(Linux4Bot instance) {
        super(instance, "notes", "save", "clear", "get");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Notes (ChatID LONG, Name varchar(4096), Text varchar(4096), Entities varchar(4096))").executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean noteExists(Long chatId, String noteName) {
        try {
            PreparedStatement ps = instance.mysql.prepareStatement("SELECT ChatID FROM Notes WHERE ChatID =" + chatId + " AND Name LIKE ?");
            ps.setString(1, noteName);
            return ps.executeQuery().next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        switch (command) {
            case "notes":
                StringBuilder response = new StringBuilder("List of notes in " + message.getChat().getTitle() + ": \n");

                try {
                    PreparedStatement ps = instance.mysql.prepareStatement("SELECT Name FROM Notes WHERE ChatID = "
                            + message.getChatId());
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        response.append("- <code>").append(rs.getString("Name")).append("</code>").append('\n');
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                response.append("You can retrieve these notes by using <code>/get notename</code>, or <code>#notename</code>");

                SendMessage sm = new SendMessage(message.getChatId().toString(), response.toString());
                sm.setParseMode("HTML");
                sm.setReplyToMessageId(message.getMessageId());
                instance.execute(sm);
                break;
            case "save":
                if (instance.enforceChatAdmin(message)) {
                    if (message.getText().trim().split(" ").length == 1) {
                        sm = new SendMessage(message.getChatId().toString(), "Name required!");
                        sm.setReplyToMessageId(message.getMessageId());
                        instance.execute(sm);
                        return;
                    } else if (message.getText().trim().split( " ").length == 2 && message.getReplyToMessage() == null) {
                        sm = new SendMessage(message.getChatId().toString(), "Text required!");
                        sm.setReplyToMessageId(message.getMessageId());;
                        instance.execute(sm);
                        return;
                    }

                    String noteName = message.getText().trim().split(" ")[1].split("\n")[0];
                    String noteText;
                    String entities;
                    if (message.getReplyToMessage() != null && message.getReplyToMessage().hasText()) {
                        noteText = message.getReplyToMessage().getText();
                        entities = MessageUtilities.entitiesToString(message.getReplyToMessage());
                    } else {
                        noteText = message.getText().trim().substring(1)
                                .substring(command.length()).trim().substring(noteName.length() + 1).trim();
                        if (noteText.startsWith("\n")) noteText = noteText.substring(1);
                        entities = MessageUtilities.entitiesToString(message);
                    }

                    try {
                        PreparedStatement ps;
                        if (noteExists(message.getChatId(), noteName)) {
                            ps = instance.mysql.prepareStatement("UPDATE Notes SET Text = ?, Entities = ? WHERE ChatID = "
                                    + message.getChatId() + " AND Name LIKE ? ");
                        } else {
                            ps = instance.mysql.prepareStatement("INSERT INTO Notes (Text, Entities, Name, ChatID) VALUES (?, ?, ?, "
                                    + message.getChatId() + ")");
                        }
                        ps.setString(1, noteText);
                        ps.setString(2, entities);
                        ps.setString(3, noteName);
                        ps.executeUpdate();

                        sm = new SendMessage(message.getChatId().toString(), "Note " + noteName + " saved!");
                        sm.setReplyToMessageId(message.getMessageId());
                        instance.execute(sm);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
                break;
            case "clear":
                String text = "Note not found!";
                if (instance.enforceChatAdmin(message)) {
                    String noteName = message.getText().trim().split(" ")[1].split("\n")[0];

                    if (noteExists(message.getChatId(), noteName)) {
                        try {
                            PreparedStatement ps = instance.mysql.prepareStatement("DELETE FROM Notes WHERE ChatID ="
                                    + message.getChatId() + " AND Name LIKE ?");
                            ps.setString(1, noteName);
                            ps.executeUpdate();

                            text = "Removed note " + noteName + "!";
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    sm = new SendMessage(message.getChatId().toString(), text);
                    sm.setReplyToMessageId(message.getMessageId());
                    instance.execute(sm);
                }
                break;
            case "get":
                String noteName = message.getText().trim().split(" ")[0].substring(1).split("\n")[0];
                text = "Note not found!";
                if (!message.getText().trim().startsWith("#") || noteName.length() == 0) {
                    if (message.getText().trim().split(" ").length > 1) {
                        noteName = message.getText().trim().split(" ")[1];
                    } else {
                        text = "Name required!";
                        noteName = "";
                    }
                }
                String entities = "";

                try {
                    PreparedStatement ps = instance.mysql.prepareStatement("SELECT Text, Entities FROM Notes WHERE ChatID = "
                            + message.getChatId() + " AND Name LIKE ?");
                    ps.setString(1, noteName);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        text = rs.getString("Text");
                        entities = rs.getString("Entities");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                sm = new SendMessage(message.getChatId().toString(), text);
                if (entities.length() > 0) {
                    sm.setEntities(MessageUtilities.entitiesFromString(entities));
                }
                sm.setDisableWebPagePreview(true);
                sm.setReplyToMessageId(message.getMessageId());
                instance.execute(sm);
                break;
        }
    }
}
