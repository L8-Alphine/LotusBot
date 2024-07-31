package com.lotusacademy.Events;

import com.lotusacademy.Commands.Moderation.BanCommand;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;
import org.jetbrains.annotations.NotNull;

public class ModerationEventListener extends ListenerAdapter {
    private final BanCommand banCommand;

    public ModerationEventListener(BanCommand banCommand) {
        this.banCommand = banCommand;
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
    banCommand.handleUnban(event);
    }
    public void onModalInteraction(@NotNull ModalInteraction event) {
        banCommand.handleUnbanModal(event);
    }
}
