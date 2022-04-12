package de.linux4.telegrambot;

public record CronTask(long when, Runnable task) {

}
