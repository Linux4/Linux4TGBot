package de.linux4.telegrambot.cmd;

import com.google.common.base.Joiner;
import de.linux4.telegrambot.Linux4Bot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Arrays;

public class SedCommand extends Command {

    public SedCommand(Linux4Bot instance) {
        super(instance, "sed");
    }

    @Override
    public Category getCategory() {
        return Command.CATEGORY_MISC;
    }

    @Override
    public HelpInfo getHelpInfo(String command) {
        return new HelpInfo("", "sed");
    }

    @Override
    public void execute(String command, Message message) throws TelegramApiException {
        String text = "Message required!";
        if (message.getReplyToMessage() != null) {
            text = "Expression required!";

            String[] broken = message.getText().trim().split(" ");
            boolean noCommand = broken[0].startsWith("s/") || broken[0].startsWith("'s/");
            if (broken.length > 1 || noCommand) {
                text = "Invalid expression!";
                String expr = Joiner.on(' ').join(Arrays.copyOfRange(broken, noCommand ? 0 : 1, broken.length));

                if (expr.startsWith("'") && expr.endsWith("'"))
                    expr = expr.substring(1, expr.length() - 1);

                if (expr.startsWith("s/")) {
                    String[] exprParts = expr.split("/");
                    String from = exprParts[1];
                    String to = exprParts.length > 2 ? exprParts[2] : "";

                    text = message.getReplyToMessage().getText().replaceAll(from, to);
                    if (text.trim().length() == 0)
                        text = "Resulting text can't be empty!";
                }
            }
        }

        SendMessage sm = new SendMessage(message.getChatId().toString(), text);
        if (message.getReplyToMessage() != null) sm.setReplyToMessageId(message.getReplyToMessage().getMessageId());
        instance.execute(sm);
    }
}
