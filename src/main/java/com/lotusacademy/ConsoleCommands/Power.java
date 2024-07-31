package com.lotusacademy.ConsoleCommands;

import com.lotusacademy.LotusBot;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Power extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(LotusBot.class);
    private volatile boolean running = true;
    private final JDA jda;

    public Power(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (running) {
            String input = scanner.nextLine();
            if ("stop".equals(input)) {
                logger.info("Stopping application...");
                jda.shutdown();

                while (!jda.getStatus().equals(JDA.Status.SHUTDOWN)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                        logger.error("An error occurred while waiting for bot shutdown", e);
                    }
                }
                logger.info("Bot has shut down, exiting application...");
                System.exit(0);
            }
        }
    }

    public void stopListening() {
        running = false;
    }
}