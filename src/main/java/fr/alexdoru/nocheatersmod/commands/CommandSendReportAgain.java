package fr.alexdoru.nocheatersmod.commands;

import fr.alexdoru.fkcountermod.FKCounterMod;
import fr.alexdoru.megawallsenhancementsmod.config.ConfigHandler;
import fr.alexdoru.megawallsenhancementsmod.utils.ChatUtil;
import fr.alexdoru.megawallsenhancementsmod.utils.NameUtil;
import fr.alexdoru.nocheatersmod.data.WDR;
import fr.alexdoru.nocheatersmod.data.WdredPlayers;
import fr.alexdoru.nocheatersmod.events.ReportQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.Date;

import static fr.alexdoru.megawallsenhancementsmod.utils.ChatUtil.addChatMessage;

public class CommandSendReportAgain extends CommandBase {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @Override
    public String getCommandName() {
        return "sendreportagain";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/sendreportagain <UUID> <playerName>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "Usage : " + getCommandUsage(sender)));
            return;
        }
        String uuid = args[0];
        String playername = args[1];
        WDR wdr = WdredPlayers.getWdredMap().get(uuid);
        if (wdr != null) {
            if (wdr.hasValidCheats()) {
                long time = (new Date()).getTime();
                if (FKCounterMod.preGameLobby && ConfigHandler.toggleautoreport) {
                    wdr.timestamp = time - WDR.TIME_BETWEEN_AUTOREPORT;
                    wdr.timeLastManualReport = time - WDR.TIME_BETWEEN_AUTOREPORT;
                    ChatUtil.addChatMessage(new ChatComponentText(ChatUtil.getTagNoCheaters() + EnumChatFormatting.GREEN + "Your cheating report against " + EnumChatFormatting.RED + playername
                            + EnumChatFormatting.GREEN + " will be sent during the game."));
                } else {
                    ReportQueue.INSTANCE.addPlayerToQueue(playername, true);
                    wdr.timestamp = time;
                    wdr.timeLastManualReport = time;
                }
                NameUtil.updateGameProfileAndName(playername, false);
            } else {
                ChatUtil.addChatMessage(new ChatComponentText(ChatUtil.getTagNoCheaters() + EnumChatFormatting.RED + "Those cheats aren't recognized by the mod :"
                        + EnumChatFormatting.GOLD + wdr.hacksToString() + EnumChatFormatting.RED + ", use valid cheats to use the reporting features."));
            }
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

}
