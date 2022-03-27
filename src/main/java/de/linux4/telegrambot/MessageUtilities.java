package de.linux4.telegrambot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtilities {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<MessageEntity> entitiesFromString(String entities) {
        List<MessageEntity> entityList = new ArrayList<>();
        JSONArray array = new JSONArray(entities);

        for (Object obj : array) {
            try {
                entityList.add(OBJECT_MAPPER.readValue(obj.toString(), MessageEntity.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return entityList;
    }

    public static String entitiesToString(List<MessageEntity> entities, int translateOffset) {
        JSONArray array = new JSONArray();

        if (entities != null) {
            for (MessageEntity entity : entities) {
                try {
                    entity.setOffset(entity.getOffset() + translateOffset);
                    array.put(new JSONObject(OBJECT_MAPPER.writeValueAsString(entity)));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

        return array.toString();
    }

    public static InlineKeyboardMarkup extractButtons(StringBuilder text, List<MessageEntity> entities) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*)]\\(buttonurl://(.*)\\)");
        Matcher matcher = pattern.matcher(text.toString());
        while (matcher.find()) {
            String url = matcher.group(2);
            if (url.endsWith(":same")) url = url.substring(0, url.length() - ":same".length());
            InlineKeyboardButton button = InlineKeyboardButton.builder().text(matcher.group(1)).url(url).build();

            if (!matcher.group(2).endsWith(":same") || buttons.size() == 0) {
                List<InlineKeyboardButton> newLine = new ArrayList<>();
                newLine.add(button);
                buttons.add(newLine);
            } else {
                buttons.get(buttons.size() - 1).add(button);
            }

            // Fix entity offsets / length
            for (MessageEntity entity : entities) {
                if (entity.getOffset() >= matcher.start()) {
                    if (entity.getOffset() < matcher.end()) { // Is inside button, nuke it
                        entity.setLength(0);
                        entity.setOffset(0);
                        continue;
                    }
                    entity.setOffset(entity.getOffset() - (matcher.end() - matcher.start()));;
                }
            }
        }
        // Remove buttons from text
        text.delete(0, text.length());
        text.append(matcher.replaceAll(""));

        if (buttons.size() > 0) {
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
            for (List<InlineKeyboardButton> row : buttons) {
                builder.keyboardRow(row);
            }
            return builder.build();
        }

        return null ;
    }

}
