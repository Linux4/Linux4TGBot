package de.linux4.telegrambot;

import com.hexadevlabs.gpt4all.LLModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GPT4All {

    private final LLModel model;
    private final LLModel.GenerationConfig config;

    public GPT4All(Path modelPath) {
        if (Files.exists(modelPath))
            model = new LLModel(modelPath);
        else
            model = null;

        config = LLModel.config().withNPredict(4096).build();
    }

    public String sendMessage(String prompt) {
        if (!isAvailable()) {
            return "Model is not provided, GPT4All extension isn't available!";
        }

        return model.generate(prompt, config, true);
    }

    public boolean isAvailable() {
        return model != null;
    }

}
