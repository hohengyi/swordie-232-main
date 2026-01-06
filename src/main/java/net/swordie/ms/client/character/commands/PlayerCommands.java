package net.swordie.ms.client.character.commands;

import net.swordie.ms.Server;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.scripts.ScriptType;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Rect;
import net.swordie.orm.dao.AccountDao;
import net.swordie.orm.dao.CharDao;
import net.swordie.orm.dao.SworDaoFactory;
import net.swordie.orm.dao.UserDao;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.AccountType.Player;
import static net.swordie.ms.enums.ChatType.Notice2;
import static net.swordie.ms.enums.ChatType.SpeakerChannel;

public class PlayerCommands {

    private static final CharDao charDao = (CharDao) SworDaoFactory.getByClass(Char.class);
    private static final UserDao userDao = (UserDao) SworDaoFactory.getByClass(User.class);
//    private static final AccDao = (AccountDao) SworDaoFactory.getByClass(Account.class);
//    private static final AccDao = (AccountDao) SworDaoFactory.getByClass(Account.Class);
    private static final AccountDao accountDao = (AccountDao) SworDaoFactory.getByClass(Account.class);


    @Command(names = {"check"}, requiredType = AccountType.Player)
    public static class Test extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            var c = chr.getClient();

            chr.dispose();
            Map<BaseStat, Integer> tbs = chr.getTotalBasicStats();
            StringBuilder sb = new StringBuilder();
            List<BaseStat> sortedList = Arrays.stream(BaseStat.values()).sorted(Comparator.comparing(Enum::toString)).collect(Collectors.toList());
            if (chr.getUser().getAccountType().ordinal() >= AccountType.Tester.ordinal() && Server.DEBUG) {
                for (BaseStat baseStat : sortedList) {
                    sb.append(String.format("%s = %d, ", baseStat, tbs.getOrDefault(baseStat, 0)));
                }
                chr.chatMessage(Notice2, String.format("X=%d, Y=%d, ping=%d, Stats: %s", chr.getPosition().getX(), chr.getPosition().getY(), c.getPing(), sb));
            } else {
                chr.chatMessage(Notice2, "Servertime: %s, Ping: %d, Str: %d (%d%%), Int: %d (%d%%), Dex: %d (%d%%), Luk: %d (%d%%), " +
                                "Att: %d (%d%%), Matt: %d (%d%%), HP: %d (%d%%), MP: %d (%d%%), AttSpeed: %d, EXP: %d%%, Mesos: %d%%, Drop: %d%%.",
                        FileTime.currentTime().toLocalDateTime(),
                        c.getPing(),
                        tbs.get(BaseStat.str),
                        tbs.get(BaseStat.strR),
                        tbs.get(BaseStat.inte),
                        tbs.get(BaseStat.intR),
                        tbs.get(BaseStat.dex),
                        tbs.get(BaseStat.dexR),
                        tbs.get(BaseStat.luk),
                        tbs.get(BaseStat.lukR),
                        tbs.get(BaseStat.pad),
                        tbs.get(BaseStat.padR),
                        tbs.get(BaseStat.mad),
                        tbs.get(BaseStat.madR),
                        tbs.get(BaseStat.mhp),
                        tbs.get(BaseStat.mhpR),
                        tbs.get(BaseStat.mmp),
                        tbs.get(BaseStat.mmpR),
                        tbs.get(BaseStat.booster),
                        tbs.get(BaseStat.expR),
                        tbs.get(BaseStat.mesoR),
                        tbs.get(BaseStat.dropR)
                );
            }
            ScriptManagerImpl smi = chr.getScriptManager();
            // all but field
            smi.stop(ScriptType.Portal);
            smi.stop(ScriptType.Npc);
            smi.stop(ScriptType.Reactor);
            smi.stop(ScriptType.Quest);
            smi.stop(ScriptType.Item);
            chr.setShop(null);
        }
    }

    @Command(names = {"inspect"}, requiredType = AccountType.Player)
    public static class inspect extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            // best accidental shutdown prevention system ever invented
            if (args.length < 1) {
                chr.chatMessage("@inspect");
                return;
            }
            Map<String, Object> customBindings = new HashMap<String, Object>();
            customBindings.put("chr", chr);
            customBindings.put("acc", chr.getAccount());
            chr.getScriptManager().startScript(2007, "ShowPlayerInv", ScriptType.Npc, customBindings);
        }
    }

    @Command(names = {"mobinfo", "mi"}, requiredType = AccountType.Player)
    public static class MobInfo extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            Rect rect = new Rect(
                    chr.getPosition().deepCopy().getX() - 200,
                    chr.getPosition().deepCopy().getY() - 200,
                    chr.getPosition().deepCopy().getX() + 200,
                    chr.getPosition().deepCopy().getY() + 200
            );
            Mob mob = chr.getField().getMobs().stream().filter(m -> rect.hasPositionInside(m.getPosition())).findFirst().orElse(null);
            if (mob != null) {
                Char controller = chr.getField().getLifeToControllers().get(mob);
                chr.chatMessage(SpeakerChannel, String.format("Object ID: %s | Template/Mob ID: %s | Level: %d | HP: %s/%s " +
                                "| MP: %s/%s | Left: %s | PDR: %s | MDR: %s " +
                                "| Controller: %s | Exp : %s | NX: %s",
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getObjectId()),
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getTemplateId()),
                        mob.getLevel(),
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getHp()),
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getMaxHp()),
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getMp()),
                        NumberFormat.getNumberInstance(Locale.US).format(mob.getMaxMp()),
                        mob.isLeft(),
                        mob.getPdr(),
                        mob.getMdr(),
                        controller == null ? "null" : chr.getName(),
                        mob.getForcedMobStat().getExp(),
                        mob.getNxDropAmount()
                        )
                );
            } else {
                chr.chatMessage(SpeakerChannel, "Could not find mob.");
            }
        }
    }

    @Command(names = {"lifeinfo", "li"}, requiredType = AccountType.Player)
    public static class LifeInfo extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            var ordereLifes = chr.getField().getLifes().values().stream()
                    .sorted(Comparator.comparingInt(Life::getObjectId))
                    .collect(Collectors.toList());
            for (var life : ordereLifes) {
                var str = String.format("%s", life);
                if (life instanceof Mob) {
                    var mob = (Mob) life;
                    str += String.format(", MaxHP: %d, HP: %d, ", mob.getMaxHp(), mob.getHp());
                }
                chr.chatMessage(str);
            }
        }

    }

    @Command(names = {"boss", "bossinfo"}, requiredType = AccountType.Player)
    public static class BossInfo extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            var bossCds = chr.getAccount().getBossCooldowns();
            if (bossCds == null || bossCds.size() == 0) {
                chr.chatMessage("You have no bosses on cooldown.");
            } else {
                chr.chatMessage("You have the following bosses on cooldown:");
                var sortedCds = chr.getAccount().getBossCooldowns().stream().sorted().collect(Collectors.toSet());

                for (var cd : sortedCds) {
                    int totalMinutes = chr.getScriptManager().getRemainingBossCooldownMinutes(cd.getBossCooldown());
                    if (totalMinutes > 0) {
                        int hours = totalMinutes / 60;
                        int minutes = totalMinutes % 60;
                        chr.chatMessage("%s: %s hours and %s minutes.", cd.getBossCooldown(), hours, minutes);
                    }
                }
            }
        }
    }

    @Command(names = {"fixquest", "fq"}, requiredType = AccountType.Player)
    public static class Test2 extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            // Fix for Already created Zeros
            if (!chr.getQuestManager().hasQuestCompleted(QuestConstants.LAPIS_LAZULI) && JobConstants.isZero(chr.getJob())) {
                chr.chatMessage("Zero Weapon UI has been unlocked (Reopen Equipment window)");
                chr.getQuestManager().completeQuest(QuestConstants.LAPIS_LAZULI, false);
            }
            if (!chr.getQuestManager().hasQuestCompleted(QuestConstants.ZERO_LAST_QUEST) && JobConstants.isZero(chr.getJob())) {
                chr.getQuestManager().completeQuest(QuestConstants.ZERO_LAST_QUEST, false);
            }

            if (chr.getLevel() < 200) {
                chr.chatMessage("You have no quests to fix.");
                return;
            }
            if (chr.hasQuestInProgress(34307)) {
                chr.chatMessage("Fixed quest: Finding the Awakened Ones (1)");
                chr.getQuestManager().completeQuest(34307);
                return;
            }
            if (chr.getQuestManager().hasQuestCompleted(34330) && !chr.getQuestManager().hasQuestCompleted(34331)) {
                chr.chatMessage("Fixed quest: Decisive Battle. The Lucid npc should now show up.");
                chr.getQuestManager().completeQuest(34331);
                return;
            }
            if (chr.getQuestManager().hasQuestCompleted(34249) && !chr.getQuestManager().hasQuestInProgress(34250) && !chr.getQuestManager().hasQuestCompleted(34250))
            {
                chr.warp(450006000);
                chr.chatMessage("Fixed quest: Unexpected Enemy (1) should now be in progress..");
                return;
            }
            chr.chatMessage("You have no quests to fix.");
        }
    }

    @Command(names = {"qm", "quickmove", "boat"}, requiredType = AccountType.Player)
    public static class QuickMoveBoat extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            if (chr.getInstance() == null) {
                chr.getScriptManager().startScript(9072302, "9072302", ScriptType.Npc);
            } else {
                chr.chatMessage("You cannot use that right now.");
            }
        }
    }

    @Command(names = {"sale", "sell"}, requiredType = AccountType.Player)
    public static class ShopCommand extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            chr.getScriptManager().startScript(9062008, "mesoMarket", ScriptType.Npc);
        }
    }

    @Command(names = {"help", "info"}, requiredType = AccountType.Player)
    public static class HelpPlayerCommands extends PlayerCommand {
        public static void execute(Char chr, String[] args) {
            chr.chatMessage("Available commands:");
            chr.chatMessage("@check: Shows your stats and unstucks your character (if you can't use skills).");
            chr.chatMessage("@mobinfo/@mi: Shows information about the mob closest to you.");
            chr.chatMessage("@boss: Shows all your boss cooldowns.");
            chr.chatMessage("@fixquest/@fq: Attempts to fix certain Arcane River quests.");
            chr.chatMessage("@lifeinfo/@li: Shows info about all lifes in the current map (pets, mobs, npcs, etc...).");
            chr.chatMessage("@qm/@quickmove/@boat: Opens the boat npc from the quick move menu.");
            chr.chatMessage("@sell/@sale: Opens the Inventory Utility npc.");
            chr.chatMessage("@inspect: Opens an npc where you can see others' equipped items and their stats.");
            chr.chatMessage("@players/checkmap: Shows you who are currently in your map.");
        }
    }

    @Command(names = {"checkmap", "players"}, requiredType = Player)
    public static class CheckMap extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            var field = chr.getField();
            var chars = new StringBuilder();
            for (var aChar : field.getChars()) {
                if(aChar.getUser().getAccountType() == Player) {
                    chars.append(aChar.getName().toUpperCase()).append(", ");
                }
            }

            chr.chatMessage(chars.substring(0, chars.length() - 2));
        }
    }

    @Command(names = {"rebirth"}, requiredType = AccountType.Player)
    public static class Rebirth extends PlayerCommand {

        public static void execute(Char chr, String[] args) {

            int level = chr.getAvatarData().getCharacterStat().getLevel();
            Account acc = chr.getAccount();

            if (level < 120) {
                chr.chatMessage("You must be at least level 120 to rebirth.");
                return;
            }

            // Determine rebirth tier
            if (level >= 300) {
                chr.addRb300();
                chr.chatMessage("You rebirthed at Level 300! (Max Level Rebirth)");
                acc.recalcAccountRebirthTotals();

            } else if (level >= 250) {
                chr.addRb250();
                chr.chatMessage("You rebirthed at Level 250!");
                acc.recalcAccountRebirthTotals();

            } else if (level >= 200) {
                chr.addRb200();
                chr.chatMessage("You rebirthed at Level 200!");
                acc.recalcAccountRebirthTotals();

            } else {
                chr.addRb120();
                chr.chatMessage("You rebirthed at Level 120!");
                // Account-wide rebirth
                acc.recalcAccountRebirthTotals();
            }

            // Reset level & EXP (Swordie way)
            chr.getAvatarData().getCharacterStat().setLevel((short) 10);
            chr.getAvatarData().getCharacterStat().setExp(0);

            // ✅ Swordie-correct level reset
            chr.setStatAndSendPacket(Stat.level, 10);
            chr.setStatAndSendPacket(Stat.exp, 0);
            chr.getJobHandler().handleLevelUp();



//            // Recalculate stats properly
//            chr.getAvatarData().getCharacterStat().recalcBaseStats();
//            chr.updateStats();

            // Warp to Henesys (safe map)
//            chr.warp(100000000);

            // Save character
            charDao.saveOrUpdate(chr);
            accountDao.saveOrUpdate(chr.getUser(), chr.getAccount(), chr);


            chr.chatMessage("Rebirth complete. Welcome back to Level 10.");
        }
    }

    @Command(names = {"rbstats", "rb"}, requiredType = AccountType.Player)
    public static class RebirthStats extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            int expRate = chr.getTotalStat(BaseStat.expR);
            int dropRate = chr.getTotalStat(BaseStat.dropR);
            int mesoRate = chr.getTotalStat(BaseStat.mesoR);
            int buffDuration = chr.getTotalStat(BaseStat.buffTimeR);
            int summonDuration = chr.getTotalStat(BaseStat.summonTimeR);
            int runeBuffDuration = chr.getTotalStat(BaseStat.runeBuffTimerR);
            int ied = chr.getTotalStat(BaseStat.ied);
            int bossDamage = chr.getTotalStat(BaseStat.bd);
            int critRate = chr.getTotalStat(BaseStat.cr);
            int critDamage = chr.getTotalStat(BaseStat.crDmg);
            int finalDamage = chr.getTotalStat(BaseStat.fd);
            int coolDownReduction = chr.getTotalStat(BaseStat.reduceCooltimeR);
            int damage = chr.getTotalStat(BaseStat.damR);
            int allStatResistance = chr.getTotalStat(BaseStat.asr);
            int elementalAttributeResistance = chr.getTotalStat(BaseStat.eleAttrResistance);
            int kockbackResistance = chr.getTotalStat(BaseStat.stance);


            Account acc = chr.getAccount();



            // Character RB
            chr.chatMessage("Character Rebirths:");
            chr.chatMessage("  Lv120: %d", chr.getRb120());
            chr.chatMessage("  Lv200: %d", chr.getRb200());
            chr.chatMessage("  Lv250: %d", chr.getRb250());
            chr.chatMessage("  Lv300: %d", chr.getRb300());

            // Account RB
            chr.chatMessage("Account-wide Rebirths:");
            chr.chatMessage("  Lv120: %d", acc.getTotalRb120());
            chr.chatMessage("  Lv200: %d", acc.getTotalRb200());
            chr.chatMessage("  Lv250: %d", acc.getTotalRb250());
            chr.chatMessage("  Lv300: %d", acc.getTotalRb300());


            chr.chatMessage("[Effective Bonuses:]");
            chr.chatMessage("EXP Rate: %d%%", expRate);
            chr.chatMessage("Drop Rate: %d%%", dropRate);
            chr.chatMessage("Meso Rate: %d%%", mesoRate);
            chr.chatMessage("Buff Duration: %d%%", buffDuration);
            chr.chatMessage("Summon Duration %d%%", summonDuration);
            chr.chatMessage("Rune Buff Duration: %d%%", runeBuffDuration);
            chr.chatMessage("Ignore Enemy Defense: %d%%", ied);
            chr.chatMessage("Boss Damage: %d%%", bossDamage);
            chr.chatMessage("Crit Rate: %d%%", critRate);
            chr.chatMessage("Crit Damage: %d%%", critDamage);
            chr.chatMessage("Final Damage: %d%%", finalDamage);
            chr.chatMessage("Cooldown Reduction: %d%%", coolDownReduction);
            chr.chatMessage("Damage: %d%%", damage);
            chr.chatMessage("All Status Resistance: %d%%", allStatResistance);
            chr.chatMessage("Elemental Attribute Resistance: %d%%", elementalAttributeResistance);
            chr.chatMessage("Knockback Resistance: %d%%", kockbackResistance);




//            chr.chatMessage(
//                    "RB120: %d | RB200: %d | RB250: %d | RB300: %d",
//                    chr.getRb120(),
//                    chr.getRb200(),
//                    chr.getRb250(),
//                    chr.getRb300()
//            );
        }
    }

    @Command(names = {"apauto"}, requiredType = AccountType.Player)
    public static class AutoAssignAP extends PlayerCommand {

        public static void execute(Char chr, String[] args) {
            int ap = chr.getStat(Stat.ap);

            if (ap <= 0) {
                chr.chatMessage("You have no AP to assign.");
                return;
            }

            List<BaseStat> stats = chr.getAutoAssignStats();

            int split = ap / stats.size();
            int remainder = ap % stats.size();

            for (BaseStat stat : stats) {
                int amount = split + (remainder-- > 0 ? 1 : 0);

                chr.addStat(stat.toStat(), amount);
                chr.setStatAndSendPacket(stat.toStat(), chr.getStat(stat.toStat()));
            }

            // clear AP
            chr.setStatAndSendPacket(Stat.ap, 0);
            chr.getTemporaryStatManager().sendSetStatPacket();



            chr.chatMessage(
                    "AP auto-assigned to %s.",
                    stats.stream().map(Enum::name).collect(Collectors.joining(", "))
            );
        }
    }





}


