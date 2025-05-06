package de.linux4.telegrambot;

import com.google.common.base.Splitter;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ConcurrentLinkedQueue;

import static de.linux4.telegrambot.TelegramConstants.COMMAND_PREFIX;
import static de.linux4.telegrambot.TelegramConstants.MAX_MESSAGE_LENGTH;

public class ChatGPT {

    public record Request(Message message, String command) {}

    private final Linux4Bot instance;
    private final String accessToken;
    private final Thread handlerThread;
    private boolean stop;
    private final ConcurrentLinkedQueue<Request> queue = new ConcurrentLinkedQueue<>();

    public ChatGPT(Linux4Bot instance, String accessToken) {
        this.instance = instance;
        this.accessToken = accessToken;

        handlerThread = new Thread(this::handleRequests);
    }

    private String generateInstructions() {
        String instructions = "You are a helpful assistant.";
        try {
            instructions += "Your name is " + instance.getMe().getFirstName() + ".";
        } catch (TelegramApiException ex) {
            ex.printStackTrace();
        }

        return instructions;
    }

    public String sendMessage(String message) {
        if (!isAvailable()) {
            return "AccessToken is not provided, ChatGPT extension isn't available!";
        }

        OpenAIClient client = OpenAIOkHttpClient.builder().apiKey(accessToken).build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(generateInstructions())
                .addUserMessage(message)
                .model(ChatModel.GPT_4O_MINI)
                .build();
        ChatCompletion chatCompletion = client.chat().completions().create(params);

        return chatCompletion.choices().get(0).message().content().get();
    }

    public boolean isAvailable() {
        return accessToken != null && !accessToken.isEmpty();
    }

    public void start() {
        stop = false;
        handlerThread.start();
    }

    public void stop() {
        stop = true;
    }

    public void enqueue(Request request) {
        queue.add(request);
    }

    private void handleRequests() {
        try {
            while (!stop) {
                Request request = queue.poll();

                if (request != null) {
                    try {
                        Message message = request.message;
                        String command = request.command;

                        SendMessage sm = new SendMessage(message.getChatId().toString(), "Thinking...");
                        sm.setReplyToMessageId(message.getMessageId());
                        int editId = instance.telegramClient.execute(sm).getMessageId();

                        String prompt = message.getText();
                        if (command != null && !command.isEmpty())
                            prompt = prompt.substring(COMMAND_PREFIX.length() + command.length());

                        String repl = instance.chatGpt.sendMessage(prompt);
                        if (repl == null || repl.isEmpty()) {
                            repl = "(Empty response from ChatGPT)";
                        }

                        boolean first = true;
                        for (String part : Splitter.fixedLength(MAX_MESSAGE_LENGTH).split(repl)) {
                            if (first) {
                                EditMessageText em = new EditMessageText(part);
                                em.setChatId(message.getChatId().toString());
                                em.setMessageId(editId);
                                em.enableMarkdown(true);
                                instance.telegramClient.execute(em);

                                first = false;
                            } else {
                                SendMessage msg = new SendMessage(message.getChatId().toString(), repl);
                                msg.setReplyToMessageId(editId);
                                msg.enableMarkdown(true);
                                editId = instance.telegramClient.execute(sm).getMessageId();
                            }
                        }
                    } catch (TelegramApiException ex) {
                        ex.printStackTrace();
                    }
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
