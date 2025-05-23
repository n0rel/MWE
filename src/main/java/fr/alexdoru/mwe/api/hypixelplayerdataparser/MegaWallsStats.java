package fr.alexdoru.mwe.api.hypixelplayerdataparser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.alexdoru.mwe.chat.ChatUtil;
import fr.alexdoru.mwe.enums.MWClass;
import fr.alexdoru.mwe.utils.ColorUtil;
import fr.alexdoru.mwe.utils.JsonUtil;
import fr.alexdoru.mwe.utils.StringUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import javax.annotation.Nullable;
import java.util.*;

public class MegaWallsStats {

    /**
     * First element of the array is the prestige level, second element is the amount of classpoints
     */
    private final LinkedHashMap<String, Classpoints> classpointsMap = new LinkedHashMap<>();
    private String chosen_class;
    private String chosen_skin_class;
    private int coins;
    private int mythic_favor;
    private int wither_damage;
    private int wither_kills;
    private int wins;
    private int wins_face_off;
    private int wins_practice;
    private int losses;
    private int kills;
    private int defender_kills;
    //private int assists; // useless
    private int deaths;
    private int final_kills;
    //private int final_kills_standard; // useless
    private int final_assists; // useless
    //private int final_assists_standard; // useless
    private int final_deaths;
    private float kdr;
    private float fkdr;
    private float wlr;
    private float fkadr;
    private float wither_damage_game;
    private float def_kill_game;
    private int time_played;
    private int nbprestiges;
    private int total_classpoints;
    private Set<String> legendarySkinsSet = null;

    private int games_played;
    private float fkpergame;

    private JsonObject classesdata = null;

    public MegaWallsStats(JsonObject playerData) {

        if (playerData == null) {
            return;
        }
        final JsonObject statsObj = JsonUtil.getJsonObject(playerData, "stats");
        if (statsObj == null) {
            return;
        }
        final JsonObject megaWallsStatsObj = JsonUtil.getJsonObject(statsObj, "Walls3");
        if (megaWallsStatsObj == null) {
            return;
        }

        chosen_class = JsonUtil.getString(megaWallsStatsObj, "chosen_class");
        chosen_skin_class = JsonUtil.getString(megaWallsStatsObj, "chosen_skin_" + chosen_class);
        coins = JsonUtil.getInt(megaWallsStatsObj, "coins");
        mythic_favor = JsonUtil.getInt(megaWallsStatsObj, "mythic_favor");
        wither_damage = JsonUtil.getInt(megaWallsStatsObj, "wither_damage") + JsonUtil.getInt(megaWallsStatsObj, "witherDamage"); // add both to get wither damage
        wither_kills = JsonUtil.getInt(megaWallsStatsObj, "wither_kills");
        wins = JsonUtil.getInt(megaWallsStatsObj, "wins");
        wins_face_off = JsonUtil.getInt(megaWallsStatsObj, "wins_face_off");
        wins_practice = JsonUtil.getInt(megaWallsStatsObj, "wins_practice");
        losses = JsonUtil.getInt(megaWallsStatsObj, "losses");
        kills = JsonUtil.getInt(megaWallsStatsObj, "kills");
        defender_kills = JsonUtil.getInt(megaWallsStatsObj, "defender_kills");
        //assists = JsonUtil.getInt(megaWallsStatsObj,"assists");
        deaths = JsonUtil.getInt(megaWallsStatsObj, "deaths");
        final_kills = JsonUtil.getInt(megaWallsStatsObj, "final_kills");
        //final_kills_standard = JsonUtil.getInt(megaWallsStatsObj,"final_kills_standard");
        final_assists = JsonUtil.getInt(megaWallsStatsObj, "final_assists");
        //final_assists_standard = JsonUtil.getInt(megaWallsStatsObj,"final_assists_standard");
        final_deaths = JsonUtil.getInt(megaWallsStatsObj, "final_deaths") + JsonUtil.getInt(megaWallsStatsObj, "finalDeaths");

        kdr = (float) kills / (deaths == 0 ? 1 : (float) deaths);
        fkdr = (float) final_kills / (final_deaths == 0 ? 1 : (float) final_deaths);
        wlr = (float) wins / (losses == 0 ? 1 : (float) losses);
        fkadr = (float) (final_kills + final_assists) / (final_deaths == 0 ? 1 : (float) final_deaths);

        time_played = JsonUtil.getInt(megaWallsStatsObj, "time_played"); // in minutes
        games_played = wins + losses; // doesn't count the draws
        fkpergame = (float) final_kills / (games_played == 0 ? 1 : (float) games_played);
        wither_damage_game = wither_damage / (games_played == 0 ? 1 : (float) games_played);
        def_kill_game = defender_kills / (games_played == 0 ? 1 : (float) games_played);

        // computes the number of prestiges
        classesdata = JsonUtil.getJsonObject(megaWallsStatsObj, "classes");
        if (classesdata != null) {
            for (final MWClass mwclass : MWClass.values()) {
                final String classname = mwclass.className.toLowerCase();
                final JsonObject classeobj = JsonUtil.getJsonObject(classesdata, classname);
                final boolean unlocked;
                final int prestige;
                if (classeobj != null) {
                    prestige = JsonUtil.getInt(classeobj, "prestige");
                    unlocked = JsonUtil.getBoolean(classeobj, "unlocked");
                } else {
                    prestige = 0;
                    unlocked = false;
                }
                nbprestiges += prestige;
                final int classpoints = MegaWallsClassStats.computeClasspoints(megaWallsStatsObj, classname);
                total_classpoints += classpoints;
                if (unlocked || classpoints != 0) {
                    classpointsMap.put(classname, new Classpoints(prestige, classpoints));
                }
            }
        }

        try {
            final JsonArray achievementsOneTimeArray = JsonUtil.getJsonArray(playerData, "achievementsOneTime");
            if (achievementsOneTimeArray == null) {
                return;
            }
            for (final JsonElement jsonElement : achievementsOneTimeArray) {
                if (jsonElement.isJsonArray()) {
                    continue;
                }
                final String achievementName = jsonElement.getAsString();
                if (achievementName != null && achievementName.startsWith("walls3_legendary_")) {
                    if (legendarySkinsSet == null) {
                        legendarySkinsSet = new HashSet<>();
                    }
                    legendarySkinsSet.add(achievementName.replace("walls3_legendary_", ""));
                }
            }
        } catch (Exception ignored) {}

    }

    public float getFkdr() {
        return fkdr;
    }

    public float getWlr() {
        return wlr;
    }

    public float getFkpergame() {
        return fkpergame;
    }

    public int getGamesPlayed() {
        return games_played;
    }

    public int getLegSkins() {
        if (legendarySkinsSet == null) {
            return 0;
        }
        return legendarySkinsSet.size();
    }

    @Nullable
    public JsonObject getClassesdata() {
        return classesdata;
    }

    public void printClassPointsMessage(String formattedname, String playername) {
        final IChatComponent imsg = new ChatComponentText(EnumChatFormatting.AQUA + ChatUtil.bar() + "\n")
                .appendSibling(ChatUtil.PlanckeHeaderText(formattedname, playername, " - Mega Walls Classpoints\n\n"));
        for (final Map.Entry<String, Classpoints> entry : classpointsMap.entrySet()) {
            final String classname = entry.getKey();
            final int prestige = entry.getValue().prestige;
            final int classpoints = entry.getValue().classpoints;
            imsg.appendSibling(new ChatComponentText(EnumChatFormatting.GREEN + StringUtil.uppercaseFirstLetter(classname))
                    .setChatStyle(new ChatStyle()
                            .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Click for " + classname + " stats")))
                            .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/plancke " + playername + " mw " + classname))));
            if (prestige != 0) {
                imsg.appendText(EnumChatFormatting.GOLD + " P" + ChatUtil.intToRoman(prestige));
            }
            imsg.appendText(" : " + ColorUtil.getPrestige4Color(classpoints) + classpoints + "\n");
        }
        imsg.appendText(EnumChatFormatting.GREEN + "Total class points : " + EnumChatFormatting.GOLD + total_classpoints + "\n");
        final int AMOUNT_OF_KITS = MWClass.values().length;
        int cpMissingForP5 = AMOUNT_OF_KITS * 5000;
        int coinsMissingForP5 = AMOUNT_OF_KITS * 3_000_000 - this.coins;
        int cpMissingForP4 = AMOUNT_OF_KITS * 2000;
        int coinsMissingForP4 = AMOUNT_OF_KITS * 2_000_000 - this.coins;
        int baseCoinsBonus = 0;
        for (final Map.Entry<String, Classpoints> entry : classpointsMap.entrySet()) {
            final int prestige = entry.getValue().prestige;
            final int cp = entry.getValue().classpoints;
            cpMissingForP5 -= Math.min(cp, 5000);
            cpMissingForP4 -= Math.min(cp, 2000);
            if (prestige == 5) {
                coinsMissingForP5 -= 3_000_000;
                coinsMissingForP4 -= 2_000_000;
                baseCoinsBonus += 3;
            } else if (prestige == 4) {
                coinsMissingForP5 -= 2_000_000;
                coinsMissingForP4 -= 2_000_000;
                baseCoinsBonus += 1;
            } else if (prestige == 3) {
                coinsMissingForP5 -= 1_250_000;
                coinsMissingForP4 -= 1_250_000;
            } else if (prestige == 2) {
                coinsMissingForP5 -= 750_000;
                coinsMissingForP4 -= 750_000;
            } else if (prestige == 1) {
                coinsMissingForP5 -= 250_000;
                coinsMissingForP4 -= 250_000;
            }
        }
        coinsMissingForP5 = Math.max(0, coinsMissingForP5);
        coinsMissingForP4 = Math.max(0, coinsMissingForP4);
        imsg.appendText(EnumChatFormatting.GREEN + "Base coins bonus : " + EnumChatFormatting.GOLD + "+" + baseCoinsBonus + "\n");
        imsg.appendText(EnumChatFormatting.GREEN + "Missing " + EnumChatFormatting.GOLD + ChatUtil.formatInt(cpMissingForP4) + " cp"
                + EnumChatFormatting.GREEN + ", "
                + EnumChatFormatting.GOLD + ChatUtil.formatInt(coinsMissingForP4) + " coins" + EnumChatFormatting.GREEN + " for all PIV\n");
        imsg.appendText(EnumChatFormatting.GREEN + "Missing " + EnumChatFormatting.GOLD + ChatUtil.formatInt(cpMissingForP5) + " cp"
                + EnumChatFormatting.GREEN + ", "
                + EnumChatFormatting.GOLD + ChatUtil.formatInt(coinsMissingForP5) + " coins" + EnumChatFormatting.GREEN + " for all PV\n");
        imsg.appendText(EnumChatFormatting.AQUA + ChatUtil.bar());
        ChatUtil.addChatMessage(imsg);
    }

    public void printGeneralStatsMessage(String formattedname, String playername) {

        final String[][] matrix1 = {
                {
                        EnumChatFormatting.AQUA + "Kills : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(kills) + " ",
                        EnumChatFormatting.AQUA + "Deaths : " + EnumChatFormatting.RED + ChatUtil.formatInt(deaths) + " ",
                        EnumChatFormatting.AQUA + "K/D Ratio : " + (kdr > 1 ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + String.format("%.3f", kdr)
                },

                {
                        EnumChatFormatting.AQUA + "Final Kills : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(final_kills) + " ",
                        EnumChatFormatting.AQUA + "Final Deaths : " + EnumChatFormatting.RED + ChatUtil.formatInt(final_deaths) + " ",
                        EnumChatFormatting.AQUA + "FK/D Ratio : " + (fkdr > 1 ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + String.format("%.3f", fkdr)
                },

                {
                        EnumChatFormatting.AQUA + "Wins : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(wins) + " ",
                        EnumChatFormatting.AQUA + "Losses : " + EnumChatFormatting.RED + ChatUtil.formatInt(losses) + " ",
                        EnumChatFormatting.AQUA + "W/L Ratio : " + (wlr > 0.33f ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + String.format("%.3f", wlr)
                },

                {
                        EnumChatFormatting.AQUA + "FA : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(final_assists) + " ",
                        EnumChatFormatting.AQUA + "FKA/D Ratio : " + (fkadr > 1f ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + String.format("%.1f", fkadr),
                        ""
                }
        };

        final String[][] matrix2 = {
                {
                        EnumChatFormatting.AQUA + "Games played : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(games_played) + "     ",
                        EnumChatFormatting.AQUA + "FK/game : " + (fkpergame > 1 ? EnumChatFormatting.GOLD : EnumChatFormatting.RED) + String.format("%.3f", fkpergame)
                },

                {
                        EnumChatFormatting.AQUA + "Wither damage : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(wither_damage) + "     ",
                        EnumChatFormatting.AQUA + "Defending Kills : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(defender_kills)
                },

                {
                        EnumChatFormatting.AQUA + "Wither dmg/game : " + EnumChatFormatting.GOLD + ((int) wither_damage_game) + "     ",
                        EnumChatFormatting.AQUA + "Def Kills/game : " + EnumChatFormatting.GOLD + String.format("%.2f", def_kill_game)
                },

                {
                        EnumChatFormatting.AQUA + "Wither kills : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(wither_kills) + "     ",
                        EnumChatFormatting.AQUA + "Leg skins : " + (getLegSkins() == 27 ? EnumChatFormatting.GOLD : EnumChatFormatting.GREEN) + getLegSkins() + EnumChatFormatting.GOLD + "/27"
                },

                {
                        EnumChatFormatting.AQUA + "Faceoff wins : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(wins_face_off) + "     ",
                        EnumChatFormatting.AQUA + "Practice wins : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(wins_practice)
                },

                {
                        EnumChatFormatting.AQUA + "Coins : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(coins) + "     ",
                        EnumChatFormatting.AQUA + "Mythic favors : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(mythic_favor)
                }
        };

        ChatUtil.addChatMessage(new ChatComponentText(EnumChatFormatting.AQUA + ChatUtil.bar() + "\n")
                .appendSibling(ChatUtil.PlanckeHeaderText(formattedname, playername, " - Mega Walls stats"))
                .appendText("\n" + "\n" + ChatUtil.alignText(matrix1) + "\n" + ChatUtil.alignText(matrix2) + "\n")
                .appendText(ChatUtil.centerLine(EnumChatFormatting.GREEN + "Prestiges : " + EnumChatFormatting.GOLD + ChatUtil.formatInt(nbprestiges) + " "
                        + EnumChatFormatting.GREEN + " Playtime (approx.) : " + EnumChatFormatting.GOLD + String.format("%.2f", time_played / 60f) + "h") + "\n")
                .appendSibling(new ChatComponentText(
                        ChatUtil.centerLine(EnumChatFormatting.GREEN + "Selected class : " + EnumChatFormatting.GOLD + (chosen_class == null ? "None" : chosen_class) + " "
                                + EnumChatFormatting.GREEN + " Selected skin : " + EnumChatFormatting.GOLD + (chosen_skin_class == null ? (chosen_class == null ? "None" : chosen_class) : chosen_skin_class)) + "\n")
                        .setChatStyle(new ChatStyle()
                                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.YELLOW + "Click for this class' stats")))
                                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/plancke " + playername + " mw " + (chosen_class == null ? "None" : chosen_class)))))
                .appendText(EnumChatFormatting.AQUA + ChatUtil.bar()));
    }

    public void printLegendaryMessage(String formattedname, String playername) {
        final IChatComponent imsg = new ChatComponentText(EnumChatFormatting.AQUA + ChatUtil.bar() + "\n")
                .appendSibling(ChatUtil.PlanckeHeaderText(formattedname, playername, " - Mega Walls Legendary skins\n"));
        final List<String> obtainedLegs = new ArrayList<>();
        final List<String> missingLegs = new ArrayList<>();
        for (final MWClass mwClass : MWClass.values()) {
            if (legendarySkinsSet != null && legendarySkinsSet.contains(mwClass.className.toLowerCase())) {
                obtainedLegs.add(mwClass.className);
            } else {
                missingLegs.add(mwClass.className);
            }
        }
        if (!obtainedLegs.isEmpty()) {
            imsg.appendText("\n" + EnumChatFormatting.GOLD + "Obtained (" + obtainedLegs.size() + ")\n");
            for (int i = 0; i < obtainedLegs.size(); i++) {
                final String s = EnumChatFormatting.GREEN + obtainedLegs.get(i) + (i == obtainedLegs.size() - 1 ? "" : ", ");
                imsg.appendText(s);
            }
        }
        if (!missingLegs.isEmpty()) {
            imsg.appendText("\n\n" + EnumChatFormatting.GOLD + "Missing (" + missingLegs.size() + ")\n");
            for (int i = 0; i < missingLegs.size(); i++) {
                final String s = EnumChatFormatting.GREEN + missingLegs.get(i) + (i == missingLegs.size() - 1 ? "" : ", ");
                imsg.appendText(s);
            }
        }
        imsg.appendText(EnumChatFormatting.AQUA + ChatUtil.bar());
        ChatUtil.addChatMessage(imsg);
    }

    private static class Classpoints {
        public final int prestige;
        public final int classpoints;

        private Classpoints(int prestige, int classpoints) {
            this.prestige = prestige;
            this.classpoints = classpoints;
        }
    }

}
