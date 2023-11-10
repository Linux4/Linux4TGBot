package de.linux4.telegrambot;

import com.google.common.base.Splitter;
import com.hexadevlabs.gpt4all.LLModel;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static de.linux4.telegrambot.TelegramConstants.COMMAND_PREFIX;
import static de.linux4.telegrambot.TelegramConstants.MAX_MESSAGE_LENGTH;

public class GPT4All {

    public record Request(Message message, String command) {}

    private final Linux4Bot instance;
    private final LLModel model;
    private final LLModel.GenerationConfig config;
    private final Thread handlerThread;
    private boolean stop;
    private final ConcurrentLinkedQueue<Request> queue = new ConcurrentLinkedQueue<>();

    public GPT4All(Linux4Bot instance, Path modelPath) {
        this.instance = instance;

        if (Files.exists(modelPath)) {
            model = new LLModel(modelPath);
            model.setThreadCount(Runtime.getRuntime().availableProcessors());
        } else
            model = null;

        config = LLModel.config().withNPredict(4096).build();
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
            return "Model is not provided, GPT4All extension isn't available!";
        }

        LLModel.ChatCompletionResponse response = model.chatCompletion(
                List.of(Map.of("role", "system", "content", generateInstructions()),
                        Map.of("role", "user", "content", message)), config);

        return response.choices.get(0).get("content");
    }

    public boolean isAvailable() {
        return model != null;
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
                    Message message = request.message;
                    String command = request.command;

                    SendMessage sm = new SendMessage(message.getChatId().toString(), "Thinking...");
                    sm.setReplyToMessageId(message.getMessageId());
                    int editId = instance.execute(sm).getMessageId();

                    String prompt = message.getText();
                    if (command != null && !command.isEmpty())
                        prompt = prompt.substring(COMMAND_PREFIX.length() + command.length());

                    String repl = instance.gpt4All.sendMessage(prompt);
                    if (repl == null || repl.isEmpty()) {
                        repl = "(Empty response from GPT4All)";
                    }

                    boolean first = true;
                    for (String part : Splitter.fixedLength(MAX_MESSAGE_LENGTH).split(repl)) {
                        if (first) {
                            EditMessageText em = new EditMessageText(part);
                            em.setChatId(message.getChatId().toString());
                            em.setMessageId(editId);
                            em.enableMarkdown(true);
                            instance.execute(em);

                            first = false;
                        } else {
                            SendMessage msg = new SendMessage(message.getChatId().toString(), repl);
                            msg.setReplyToMessageId(editId);
                            msg.enableMarkdown(true);
                            editId = instance.execute(sm).getMessageId();
                        }
                    }
                }

                Thread.sleep(100);
            }
        } catch (InterruptedException | TelegramApiException ex) {
            ex.printStackTrace();
        }
    }
}
