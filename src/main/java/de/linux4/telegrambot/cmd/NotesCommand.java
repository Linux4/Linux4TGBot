package de.linux4.telegrambot.cmd;

import de.linux4.telegrambot.Linux4Bot;
import de.linux4.telegrambot.MessageUtilities;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotesCommand extends Command {

    private static final String TYPE_AUDIO = "audio";
    private static final String TYPE_DOCUMENT = "document";
    private static final String TYPE_PHOTO = "photo";
    private static final String TYPE_STICKER = "sticker";
    private static final String TYPE_TEXT = "text";
    private static final String TYPE_VIDEO = "video";

    public NotesCommand(Linux4Bot instance) {
        super(instance, "notes", "save", "clear", "get");

        try {
            instance.mysql.prepareStatement("CREATE TABLE IF NOT EXISTS Notes (ChatID LONG, Name varchar(4096), Text varchar(4096), Entities varchar(4096))")
                    .executeUpdate();
            instance.mysql.prepareStatement("ALTER TABLE Notes ADD COLUMN IF NOT EXISTS Type VARCHAR(255) DEFAULT 'text' AFTER Name")
                    .executeUpdate();
            instance.mysql.prepareStatement("ALTER TABLE Notes ADD COLUMN IF NOT EXISTS FileID Varchar(255)")
                    .executeUpdate();
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
                    } else if (message.getText().trim().split(" ").length == 2 && message.getReplyToMessage() == null) {
                        sm = new SendMessage(message.getChatId().toString(), "Text required!");
                        sm.setReplyToMessageId(message.getMessageId());
                        ;
                        instance.execute(sm);
                        return;
                    }

                    String noteName = message.getText().trim().split(" ")[1].split("\n")[0];
                    String noteText = "";
                    String noteType = TYPE_TEXT;
                    String fileID = "";
                    String entities;
                    if (message.getReplyToMessage() != null) {
                        if (message.getReplyToMessage().hasText()) {
                            noteText = message.getReplyToMessage().getText();
                        }
                        if (message.getReplyToMessage().hasAudio()) {
                            noteType = TYPE_AUDIO;
                            fileID = message.getReplyToMessage().getAudio().getFileId();
                        } else if (message.getReplyToMessage().hasDocument()) {
                            noteType = TYPE_DOCUMENT;
                            fileID = message.getReplyToMessage().getDocument().getFileId();
                        } else if (message.getReplyToMessage().hasPhoto()) {
                            noteType = TYPE_PHOTO;
                            fileID = message.getReplyToMessage().getPhoto().get(2).getFileId();
                        } else if (message.getReplyToMessage().hasSticker()) {
                            noteType = TYPE_STICKER;
                            fileID = message.getReplyToMessage().getSticker().getFileId();
                        } else if (message.getReplyToMessage().hasVideo()) {
                            noteType = TYPE_VIDEO;
                            fileID = message.getReplyToMessage().getVideo().getFileId();
                        }
                        if (noteType.equals(TYPE_TEXT))
                            entities = MessageUtilities.entitiesToString(message.getReplyToMessage().getEntities(), 0);
                        else {
                            noteText = message.getReplyToMessage().getCaption();
                            entities = MessageUtilities.entitiesToString(message.getReplyToMessage().getCaptionEntities(), 0);
                        }
                    } else {
                        if (message.hasText()) {
                            noteText = message.getText().trim().substring(1)
                                    .substring(command.length()).trim().substring(noteName.length() + 1).trim();
                        }
                        if (message.hasAudio()) {
                            noteType = TYPE_AUDIO;
                            fileID = message.getAudio().getFileId();
                        } else if (message.hasDocument()) {
                            noteType = TYPE_DOCUMENT;
                            fileID = message.getDocument().getFileId();
                        } else if (message.hasPhoto()) {
                            noteType = TYPE_PHOTO;
                            fileID = message.getPhoto().get(2).getFileId();
                        } else if (message.hasSticker()) {
                            noteType = TYPE_STICKER;
                            fileID = message.getSticker().getFileId();
                        } else if (message.hasVideo()) {
                            noteType = TYPE_VIDEO;
                            fileID = message.getVideo().getFileId();
                        }
                        if (noteText.startsWith("\n")) noteText = noteText.substring(1);
                        if (noteType.equals(TYPE_TEXT))
                            entities = MessageUtilities.entitiesToString(message.getEntities(),
                                    noteText.length() - message.getText().length());
                        else {
                            noteText = message.getCaption().trim().substring(1).substring(command.length()).trim()
                                    .substring(noteName.length() + 1).trim();
                            entities = MessageUtilities.entitiesToString(message.getCaptionEntities(),
                                    noteText.length() - message.getCaption().length());
                        }
                    }

                    try {
                        PreparedStatement ps;
                        if (noteExists(message.getChatId(), noteName)) {
                            ps = instance.mysql.prepareStatement("UPDATE Notes SET Text = ?, Type = ?, Entities = ?, FileID = ? WHERE ChatID = "
                                    + message.getChatId() + " AND Name LIKE ? ");
                        } else {
                            ps = instance.mysql.prepareStatement("INSERT INTO Notes (Text, Type, Entities, FileID, ChatID, Name) VALUES (?, ?, ?, ?,"
                                    + message.getChatId() + ", ?)");
                        }
                        ps.setString(1, noteText);
                        ps.setString(2, noteType);
                        ps.setString(3, entities);
                        ps.setString(4, fileID);
                        ps.setString(5, noteName);
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
                String noteType = TYPE_TEXT;
                String fileID = "";
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
                    PreparedStatement ps = instance.mysql.prepareStatement("SELECT Type, Text, Entities, FileID FROM Notes WHERE ChatID = "
                            + message.getChatId() + " AND Name LIKE ?");
                    ps.setString(1, noteName);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        noteType = rs.getString("Type");
                        text = rs.getString("Text");
                        entities = rs.getString("Entities");
                        fileID = rs.getString("FileID");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                List<MessageEntity> msgEntities = entities.trim().length() > 0 ? MessageUtilities.entitiesFromString(entities)
                        : new ArrayList<>();
                StringBuilder msgText = new StringBuilder(text);
                InlineKeyboardMarkup keyboard = MessageUtilities.extractButtons(msgText, msgEntities);
                text = msgText.toString();
                // Putting keyboard removed all text
                if (keyboard != null && text.trim().length() == 0) text = noteName;

                switch (noteType) {
                    case TYPE_AUDIO:
                        SendAudio sa = new SendAudio(message.getChatId().toString(), new InputFile(fileID));
                        if (entities.length() > 0) {
                            sa.setCaptionEntities(msgEntities);
                        }
                        sa.setCaption(text);
                        sa.setReplyToMessageId(message.getMessageId());
                        sa.setReplyMarkup(keyboard);
                        instance.execute(sa);
                        break;
                    case TYPE_DOCUMENT:
                        SendDocument sd = new SendDocument(message.getChatId().toString(), new InputFile(fileID));
                        if (entities.length() > 0) {
                            sd.setCaptionEntities(msgEntities);
                        }
                        sd.setCaption(text);
                        sd.setReplyToMessageId(message.getMessageId());
                        sd.setReplyMarkup(keyboard);
                        instance.execute(sd);
                        break;
                    case TYPE_PHOTO:
                        SendPhoto sp = new SendPhoto(message.getChatId().toString(), new InputFile(fileID));
                        if (entities.length() > 0) {
                            sp.setCaptionEntities(msgEntities);
                        }
                        sp.setCaption(text);
                        sp.setReplyToMessageId(message.getMessageId());
                        sp.setReplyMarkup(keyboard);
                        instance.execute(sp);
                        break;
                    case TYPE_STICKER:
                        SendSticker ss = new SendSticker(message.getChatId().toString(), new InputFile(fileID));
                        ss.setReplyToMessageId(message.getMessageId());
                        ss.setReplyMarkup(keyboard);
                        instance.execute(ss);
                        break;
                    case TYPE_TEXT:
                        sm = new SendMessage(message.getChatId().toString(), text);
                        if (entities.length() > 0) {
                            sm.setEntities(msgEntities);
                        }
                        sm.setDisableWebPagePreview(true);
                        sm.setReplyToMessageId(message.getMessageId());
                        sm.setReplyMarkup(keyboard);
                        instance.execute(sm);
                        break;
                    case TYPE_VIDEO:
                        SendVideo sv = new SendVideo(message.getChatId().toString(), new InputFile(fileID));
                        if (entities.length() > 0) {
                            sv.setCaptionEntities(msgEntities);
                        }
                        sv.setCaption(text);
                        sv.setReplyToMessageId(message.getMessageId());
                        sv.setReplyMarkup(keyboard);
                        instance.execute(sv);
                        break;
                }
                break;
        }
    }
}
