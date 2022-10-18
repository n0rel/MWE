package fr.alexdoru.megawallsenhancementsmod.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.alexdoru.megawallsenhancementsmod.api.exceptions.ApiException;
import fr.alexdoru.megawallsenhancementsmod.api.hypixelplayerdataparser.GeneralInfo;
import fr.alexdoru.megawallsenhancementsmod.api.hypixelplayerdataparser.MegaWallsStats;
import fr.alexdoru.megawallsenhancementsmod.api.requests.HypixelPlayerData;
import fr.alexdoru.megawallsenhancementsmod.enums.MWClass;
import fr.alexdoru.megawallsenhancementsmod.fkcounter.FKCounterMod;
import fr.alexdoru.megawallsenhancementsmod.fkcounter.events.MwGameEvent;
import fr.alexdoru.megawallsenhancementsmod.nocheaters.events.GameInfoGrabber;
import fr.alexdoru.megawallsenhancementsmod.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class CommandScanGame extends CommandBase {

    private static final HashMap<UUID, ScanResult> scangameMap = new HashMap<>();
    private static String scanGameId;

    public CommandScanGame() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onMwGame(MwGameEvent event) {
        if (event.getType() == MwGameEvent.EventType.GAME_START) {
            onGameStart();
        }
        if (event.getType() == MwGameEvent.EventType.GAME_END) {
            clearScanGameData();
        }
    }

    private static void clearScanGameData() {
        scanGameId = null;
        scangameMap.clear();
    }

    private static void onGameStart() {
        final String currentGameId = GameInfoGrabber.getGameIDfromscoreboard();
        if (!currentGameId.equals("?") && scanGameId != null && !scanGameId.equals(currentGameId)) {
            clearScanGameData();
        }
    }

    public static boolean doesPlayerFlag(UUID uuid) {
        final ScanResult scanResult = scangameMap.get(uuid);
        return scanResult != null && scanResult.msg != null;
    }

    public static void put(UUID uuid, IChatComponent msg) {
        scangameMap.put(uuid, new ScanResult(msg));
    }

    @Override
    public String getCommandName() {
        return "scangame";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/scangame";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (HypixelApiKeyUtil.apiKeyIsNotSetup()) {
            ChatUtil.printApikeySetupInfo();
            return;
        }
        final String currentGameId = GameInfoGrabber.getGameIDfromscoreboard();
        final Collection<NetworkPlayerInfo> playerCollection = Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap();
        int i = 0;
        if (!currentGameId.equals("?") && currentGameId.equals(scanGameId)) {
            for (final NetworkPlayerInfo networkPlayerInfo : playerCollection) {
                final ScanResult scanResult = scangameMap.get(networkPlayerInfo.getGameProfile().getId());
                if (scanResult == null) {
                    i++;
                    Multithreading.addTaskToQueue(new ScanPlayerTask(networkPlayerInfo));
                } else if (scanResult.msg != null) {
                    ChatUtil.addChatMessage(new ChatComponentText(ChatUtil.getTagMW()).appendSibling(NameUtil.getFormattedNameWithPlanckeClickEvent(networkPlayerInfo)).appendSibling(scanResult.msg));
                }
            }
            ChatUtil.addChatMessage(ChatUtil.getTagMW() + EnumChatFormatting.GREEN + "Scanning " + i + " more players...");
        } else {
            scanGameId = GameInfoGrabber.getGameIDfromscoreboard();
            for (final NetworkPlayerInfo networkPlayerInfo : playerCollection) {
                i++;
                Multithreading.addTaskToQueue(new ScanPlayerTask(networkPlayerInfo));
            }
            ChatUtil.addChatMessage(ChatUtil.getTagMW() + EnumChatFormatting.GREEN + "Scanning " + i + " players...");
        }
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

}

class ScanResult {

    public final IChatComponent msg;

    ScanResult(IChatComponent msg) {
        this.msg = msg;
    }

}

class ScanPlayerTask implements Callable<String> {

    final NetworkPlayerInfo networkPlayerInfo;

    public ScanPlayerTask(NetworkPlayerInfo networkPlayerInfoIn) {
        this.networkPlayerInfo = networkPlayerInfoIn;
    }

    @Override
    public String call() {

        final UUID uuid = networkPlayerInfo.getGameProfile().getId();

        try {

            if (NameUtil.isntRealPlayer(uuid)) {
                CommandScanGame.put(uuid, null);
                return null;
            }

            final String playername = networkPlayerInfo.getGameProfile().getName();
            final HypixelPlayerData playerdata = new HypixelPlayerData(uuid.toString().replace("-", ""));
            final MegaWallsStats megawallsstats = new MegaWallsStats(playerdata.getPlayerData());
            IChatComponent imsg = null;

            if ((megawallsstats.getGames_played() <= 25 && megawallsstats.getFkdr() > 3.5f) ||
                    (megawallsstats.getGames_played() <= 250 && megawallsstats.getFkdr() > 5f) ||
                    (megawallsstats.getGames_played() <= 500 && megawallsstats.getFkdr() > 8f)) {

                imsg = new ChatComponentText(EnumChatFormatting.GRAY + " played : " + EnumChatFormatting.GOLD + megawallsstats.getGames_played()
                        + EnumChatFormatting.GRAY + " games, fkd : " + EnumChatFormatting.GOLD + String.format("%.1f", megawallsstats.getFkdr())
                        + EnumChatFormatting.GRAY + " FK/game : " + EnumChatFormatting.GOLD + String.format("%.1f", megawallsstats.getFkpergame())
                        + EnumChatFormatting.GRAY + " W/L : " + EnumChatFormatting.GOLD + String.format("%.1f", megawallsstats.getWlr()));

            } else if (megawallsstats.getGames_played() < 15) {

                final GeneralInfo generalInfo = new GeneralInfo(playerdata.getPlayerData());
                final boolean firstGame = megawallsstats.getGames_played() == 0;
                final boolean secondFlag = generalInfo.getCompletedQuests() < 20 && generalInfo.getNetworkLevel() > 30f;
                final JsonObject classesdata = megawallsstats.getClassesdata();

                if (FKCounterMod.isInMwGame) {

                    final ScorePlayerTeam team = Minecraft.getMinecraft().theWorld.getScoreboard().getPlayersTeam(playername);
                    final String classTag = EnumChatFormatting.getTextWithoutFormattingCodes(team.getColorSuffix().replace("[", "").replace("]", "").replace(" ", ""));
                    final MWClass mwClass = MWClass.fromTag(classTag);
                    if (mwClass != null) {
                        final JsonObject entryclassobj = classesdata.getAsJsonObject(mwClass.className.toLowerCase());
                        if (firstGame) {
                            imsg = getMsgFirstGame(mwClass.className, entryclassobj);
                        } else if (secondFlag) {
                            imsg = getMsg(mwClass.className, entryclassobj, generalInfo.getCompletedQuests(), (int) generalInfo.getNetworkLevel(), megawallsstats.getGames_played());
                        }
                    }

                } else {

                    for (final Map.Entry<String, JsonElement> entry : classesdata.entrySet()) {
                        if (entry.getValue() != null && entry.getValue().isJsonObject()) {
                            final JsonObject entryclassobj = entry.getValue().getAsJsonObject();
                            if (imsg == null) {
                                if (firstGame) {
                                    imsg = getMsgFirstGame(entry.getKey(), entryclassobj);
                                } else if (secondFlag) {
                                    imsg = getMsg(entry.getKey(), entryclassobj, generalInfo.getCompletedQuests(), (int) generalInfo.getNetworkLevel(), megawallsstats.getGames_played());
                                }
                            } else {
                                final IChatComponent classMsg = getFormattedClassMsg(entry.getKey(), entryclassobj, firstGame);
                                if (classMsg != null) {
                                    imsg.appendSibling(classMsg);
                                }
                            }
                        }
                    }

                }

            }

            if (imsg == null) {
                final float ratio = megawallsstats.getLegSkins() * 12f / (megawallsstats.getGames_played() == 0 ? 1 : megawallsstats.getGames_played());
                if (ratio >= 1) {
                    imsg = new ChatComponentText(EnumChatFormatting.GRAY + " played : " + EnumChatFormatting.GOLD + megawallsstats.getGames_played()
                            + EnumChatFormatting.GRAY + " games, and has : " + EnumChatFormatting.GOLD + megawallsstats.getLegSkins()
                            + EnumChatFormatting.GRAY + " legendary skin" + (megawallsstats.getLegSkins() > 1 ? "s" : ""));
                }
            }

            if (imsg != null) {
                ChatUtil.addChatMessage(new ChatComponentText(ChatUtil.getTagMW()).appendSibling(NameUtil.getFormattedNameWithPlanckeClickEvent(networkPlayerInfo)).appendSibling(imsg));
                CommandScanGame.put(uuid, imsg);
                NameUtil.updateGameProfileAndName(networkPlayerInfo);
                return null;
            }

        } catch (ApiException ignored) {}

        CommandScanGame.put(uuid, null);
        return null;

    }

    private IChatComponent getMsgFirstGame(String className, JsonObject entryclassobj) {
        final int skill_level_a = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_a"), 1); //skill
        final int skill_level_d = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_d"), 1); //kit
        if (skill_level_a >= 4 || skill_level_d >= 4) {
            return new ChatComponentText(EnumChatFormatting.GRAY + " never played and has :").appendSibling(getFormattedClassMsg(className, entryclassobj, true));
        }
        return null;
    }

    private IChatComponent getMsg(String className, JsonObject entryclassobj, int quests, int networklevel, int gameplayed) {
        final int skill_level_a = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_a"), 1); //skill
        final int skill_level_b = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_b"), 1); //passive1
        final int skill_level_c = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_c"), 1); //passive2
        final int skill_level_d = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_d"), 1); //kit
        if (skill_level_a == 5 && skill_level_b == 3 && skill_level_c == 3 && skill_level_d == 5) {
            return new ChatComponentText(EnumChatFormatting.GRAY + " played " + EnumChatFormatting.GOLD + gameplayed + EnumChatFormatting.GRAY + " games"
                    + EnumChatFormatting.GRAY + ", network lvl " + EnumChatFormatting.GOLD + networklevel
                    + EnumChatFormatting.GRAY + ", with " + EnumChatFormatting.GOLD + quests + EnumChatFormatting.GRAY + " quests"
                    + EnumChatFormatting.GRAY + " and has :").appendSibling(getFormattedClassMsg(className, entryclassobj, false));
        }
        return null;
    }

    private IChatComponent getFormattedClassMsg(String className, JsonObject entryclassobj, boolean firstgame) {
        final int skill_level_a = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_a"), 1); //skill
        final int skill_level_b = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_b"), 1); //passive1
        final int skill_level_c = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_c"), 1); //passive2
        final int skill_level_d = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_d"), 1); //kit
        final int skill_level_g = Math.max(JsonUtil.getInt(entryclassobj, "skill_level_g"), 1); //gathering
        if (firstgame ? skill_level_a >= 4 || skill_level_d >= 4 : skill_level_a == 5 && skill_level_b == 3 && skill_level_c == 3 && skill_level_d == 5) {
            return new ChatComponentText(" " + EnumChatFormatting.GOLD + className + " "
                    + (skill_level_d == 5 ? EnumChatFormatting.GOLD : EnumChatFormatting.DARK_GRAY) + ChatUtil.intToRoman(skill_level_d) + " "
                    + (skill_level_a == 5 ? EnumChatFormatting.GOLD : EnumChatFormatting.DARK_GRAY) + ChatUtil.intToRoman(skill_level_a) + " "
                    + (skill_level_b == 3 ? EnumChatFormatting.GOLD : EnumChatFormatting.DARK_GRAY) + ChatUtil.intToRoman(skill_level_b) + " "
                    + (skill_level_c == 3 ? EnumChatFormatting.GOLD : EnumChatFormatting.DARK_GRAY) + ChatUtil.intToRoman(skill_level_c) + " "
                    + (skill_level_g == 3 ? EnumChatFormatting.GOLD : EnumChatFormatting.DARK_GRAY) + ChatUtil.intToRoman(skill_level_g));
        }
        return null;
    }

}

