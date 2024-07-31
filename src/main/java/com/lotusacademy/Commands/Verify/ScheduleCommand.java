package com.lotusacademy.Commands.Verify;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lotusacademy.Commands.ICommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScheduleCommand implements ICommand {

    private static final String SCHEDULE_FILE = "scheduleData.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCommand.class);

    @Override
    public String getName() {
        return "schedule";
    }

    @Override
    public String getDescription() {
        return "Creates, Lists, Removes and Edits showcase schedules for the academy.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of();
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of(
                new SubcommandData("add", "Add a new schedule")
                        .addOptions(new OptionData(OptionType.STRING, "time", "Schedule time", true)),
                new SubcommandData("list", "List all schedules"),
                new SubcommandData("remove", "Remove a schedule")
                        .addOptions(new OptionData(OptionType.INTEGER, "id", "Schedule ID", true)),
                new SubcommandData("edit", "Edit a schedule")
                        .addOptions(new OptionData(OptionType.INTEGER, "id", "Schedule ID", true),
                                new OptionData(OptionType.STRING, "newtime", "New Schedule time", true))
        );
    }

    @Override
    public void execute(@NotNull SlashCommandInteractionEvent event) {
        if (event.getMember() == null || !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.reply("You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) return;

        switch (subcommand) {
            case "add":
                String time = event.getOption("time").getAsString();
                addSchedule(time, event);
                break;
            case "list":
                listSchedules(event);
                break;
            case "remove":
                int removeId = event.getOption("id").getAsInt();
                removeSchedule(removeId, event);
                break;
            case "edit":
                int editId = event.getOption("id").getAsInt();
                String newTime = event.getOption("newtime").getAsString();
                editSchedule(editId, newTime, event);
                break;
            default:
                event.reply("Unknown subcommand.").setEphemeral(true).queue();
                break;
        }
    }

    private void addSchedule(String time, SlashCommandInteractionEvent event) {
        List<String> schedules = getSchedules();
        schedules.add(time);
        saveSchedules(schedules);

        event.reply("Schedule added: " + time).setEphemeral(true).queue();
    }

    private void listSchedules(SlashCommandInteractionEvent event) {
        List<String> schedules = getSchedules();
        EmbedBuilder embed = new EmbedBuilder().setTitle("Schedules");

        StringBuilder description = new StringBuilder();
        for (int i = 0; i < schedules.size(); i++) {
            description.append(i + 1).append(". ").append(schedules.get(i)).append("\n");
        }

        embed.setDescription(description.toString());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private void removeSchedule(int id, SlashCommandInteractionEvent event) {
        List<String> schedules = getSchedules();
        if (id <= 0 || id > schedules.size()) {
            event.reply("Invalid schedule ID").setEphemeral(true).queue();
            return;
        }

        String removed = schedules.remove(id - 1);
        saveSchedules(schedules);

        event.reply("Schedule removed: " + removed).setEphemeral(true).queue();
    }

    private void editSchedule(int id, String newTime, SlashCommandInteractionEvent event) {
        List<String> schedules = getSchedules();
        if (id <= 0 || id > schedules.size()) {
            event.reply("Invalid schedule ID").setEphemeral(true).queue();
            return;
        }

        schedules.set(id - 1, newTime);
        saveSchedules(schedules);

        event.reply("Schedule updated: " + newTime).setEphemeral(true).queue();
    }

    private List<String> getSchedules() {
        File file = new File(SCHEDULE_FILE);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    saveSchedules(new ArrayList<>());
                }
            } catch (IOException e) {
                LOGGER.error("Error creating schedule data file", e);
                return new ArrayList<>();
            }
        }

        try (FileReader reader = new FileReader(SCHEDULE_FILE)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            LOGGER.error("Error reading schedule data from JSON file", e);
            return new ArrayList<>();
        }
    }

    private void saveSchedules(List<String> schedules) {
        try (FileWriter writer = new FileWriter(SCHEDULE_FILE)) {
            Gson gson = new Gson();
            gson.toJson(schedules, writer);
        } catch (IOException e) {
            LOGGER.error("Error writing schedule data to JSON file", e);
        }
    }
}
