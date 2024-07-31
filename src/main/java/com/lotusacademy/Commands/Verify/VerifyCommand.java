package com.lotusacademy.Commands.Verify;

import com.lotusacademy.Commands.ICommand;
import com.lotusacademy.Handlers.ChannelHandler;
import com.lotusacademy.Managers.TicketManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;

public class VerifyCommand extends ListenerAdapter implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyCommand.class);
    private final TicketManager ticketManager;

    public VerifyCommand(TicketManager ticketManager) {
        this.ticketManager = ticketManager;
    }

    @Override
    public String getName() {
        return "verify";
    }

    @Override
    public String getDescription() {
        return "Verifies the user to a certain role based on the verification ticket that was created.";
    }

    @Override
    public List<OptionData> getOptions() {
        return List.of(
                new OptionData(OptionType.USER, "user", "User to verify", true),
                new OptionData(OptionType.STRING, "role", "Role to assign (player, parent, scout)", true)
        );
    }

    @Override
    public List<SubcommandData> getSubcommands() {
        return List.of();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MANAGE_ROLES)) {
            event.reply("You don't have permission to verify users.").setEphemeral(true).queue();
            return;
        }

        Member targetMember = event.getOption("user").getAsMember();
        String roleType = event.getOption("role").getAsString().toLowerCase();
        String roleName;

        switch (roleType) {
            case "player":
                roleName = "Player";
                break;
            case "parent":
                roleName = "Parents/Guardians";
                break;
            case "scout":
                roleName = "Scout";
                break;
            default:
                event.reply("Invalid role type. Choose from player, parent, or scout.").setEphemeral(true).queue();
                return;
        }

        Role role = event.getGuild().getRolesByName(roleName, true).stream().findFirst().orElse(null);
        Role verifiedRole = event.getGuild().getRolesByName("Verified", true).stream().findFirst().orElse(null);

        if (role == null || verifiedRole == null) {
            event.reply("Required role(s) not found!").setEphemeral(true).queue();
            return;
        }

        event.getGuild().addRoleToMember(targetMember, role).queue();
        event.getGuild().addRoleToMember(targetMember, verifiedRole).queue();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Verification Successful")
                .setColor(Color.GREEN)
                .setDescription("Congratulations, you have been verified as a " + roleName + ".")
                .setFooter("Verification System");

        targetMember.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(embed.build()).queue());

        logVerification(event.getMember(), targetMember, roleName);

        event.reply("User " + targetMember.getAsMention() + " has been verified as a " + roleName + " and granted the Verified role.").setEphemeral(true).queue();
    }

    private void logVerification(Member verifier, Member verified, String roleName) {
        EmbedBuilder logEmbed = new EmbedBuilder()
                .setTitle("User Verified")
                .setColor(Color.BLUE)
                .setDescription(verifier.getAsMention() + " verified " + verified.getAsMention() + " as a " + roleName + ".")
                .setTimestamp(java.time.Instant.now());

        ChannelHandler.sendLogEmbed(logEmbed);
    }
}
