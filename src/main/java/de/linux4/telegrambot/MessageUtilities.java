package de.linux4.telegrambot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

import java.util.ArrayList;
import java.util.List;

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
                    array.put(OBJECT_MAPPER.writeValueAsString(entity));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

        return array.toString();
    }

}
