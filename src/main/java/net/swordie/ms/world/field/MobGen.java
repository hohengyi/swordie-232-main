package net.swordie.ms.world.field;

import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.constants.ChannelScaling;
import net.swordie.ms.constants.CustomConstants;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.MobConstants;
import net.swordie.ms.events.Events;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.mob.ForcedMobStat;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.containerclasses.MobInfo;
import net.swordie.ms.util.Position;

/**
 * @author Sjonnie
 * Created on 7/26/2018.
 */
public class MobGen extends Life {

    private MobInfo mob;
    private long nextPossibleSpawnTime = Long.MIN_VALUE;
    private boolean hasSpawned;

    public MobGen(int templateId) {
        super(templateId);
    }

    public MobInfo getMob() {
        return mob;
    }

    public void setMob(MobInfo mob) {
        this.mob = mob;
    }

    /**
     * Spawns a Mob at the position of this MobGen.
     */
    public void spawnMob(boolean buffed) {
        Mob mob = getMob().toMob();
        Field field = getField();

        Position pos = getPosition();
        mob.setPosition(pos.deepCopy());
        mob.setHomePosition(pos.deepCopy());

        Foothold fh = field.getInfo().getFootholdById(getFh());
        if (fh == null) {
            fh = field.findFootHoldBelow(pos);
        }
        if (fh == null) {
            // Edge case where the mob is spawned on some weird foothold
            return;
        }
        mob.setHomeFoothold(fh.deepCopy());
        mob.setCurFoodhold(fh.deepCopy());

        // non-elite bosses don't get buffed
        if (buffed && !mob.isBoss()) {
            int prefix = field.getId() / 1000000;
            mob.buff(MobConstants.getBuffMultiplierFromRegion(prefix));
        }

        int channel = field.getChannel();

// ================= CHANNEL SCALING =================
        if (!mob.isBoss()) {
            ForcedMobStat fms = mob.getForcedMobStat();

            // 1) HP scaling
            double hpMulti = ChannelScaling.getHpMultiplier(channel);
            long newHp = (long) (fms.getMaxHP() * hpMulti);
            fms.setMaxHP(newHp);
            mob.setHp(newHp);

            // 2) EXP scaling
            double expMulti = ChannelScaling.getExpMultiplier(channel);
            fms.setExp((long) (fms.getExp() * expMulti));

            // 3) PDR / MDR scaling (this replaces damage reduction)
            int defBonus = ChannelScaling.getDefenseBonus(channel);
            if (defBonus > 0) {
                fms.setPdr(fms.getPdr() + defBonus);
                fms.setMdr(fms.getMdr() + defBonus);
            }
        }
// =====================================================================




        field.spawnLife(mob, null);
        if (CustomConstants.AUTO_AGGRO && FieldConstants.isAggroField(field.getId())) {
            field.broadcastPacket(MobPool.forceChase(mob.getObjectId(), true));
        }
//        setNextPossibleSpawnTime(System.currentTimeMillis() + (getMob().getMobTime() * 1000));
//        int mobTimeSeconds = getMob().getMobTime();
//
//// Force fast respawn (1–2 seconds)
//        int respawnDelayMs = Math.max(1000, Math.min(mobTimeSeconds * 1000, 2000));
//
//        setNextPossibleSpawnTime(System.currentTimeMillis() + respawnDelayMs);
        setNextPossibleSpawnTime(System.currentTimeMillis() + 1000);


        setHasSpawned(true);
    }

    public MobGen deepCopy() {
        MobGen mobGen = new MobGen(getTemplateId());
        if (getMob() != null) {
            mobGen.setPosition(getPosition());
            mobGen.setMob(getMob());
        }
        return mobGen;
    }

    public boolean canSpawnOnField(Field field) {
        int currentMobs = field.getMobs().size();
        // not over max mobs, delay of spawn ended, if mobtime == -1 (not respawnable) must not have yet spawned
        // no mob in area around this, unless kishin is active
        int baseCap = field.getInfo().getFixedMobCapacity();
        int boostedCap = baseCap * 3; // 🔥 increase density here
        return canSpawnOnSpecialField(field)
                && currentMobs < boostedCap
                && getNextPossibleSpawnTime() < System.currentTimeMillis()
                && (getMob().getMobTime() != -1 || !hasSpawned()
                    || getMob().getTemplateId() == 9001005) // 2nd job canonneer quest, has a mob time of -1
                ;
    }

    private boolean canSpawnOnSpecialField(Field field) {
        switch (field.getId()) {
            case 220080100:
            case 220080200:
            case 220080300:
                // Papulatus field
                return canSpawnOnPapulatusField(field);
        }
        return true;
    }

    private boolean canSpawnOnPapulatusField(Field field) {
        return field.hasProperty("Phase") && ((int) field.getProperty("Phase")) != 1;
    }

    public long getNextPossibleSpawnTime() {
        return nextPossibleSpawnTime;
    }

    public void setNextPossibleSpawnTime(long nextPossibleSpawnTime) {
        this.nextPossibleSpawnTime = nextPossibleSpawnTime;
    }

    public boolean hasSpawned() {
        return hasSpawned;
    }

    public void setHasSpawned(boolean hasSpawned) {
        this.hasSpawned = hasSpawned;
    }

    @Override
    public String toString() {
        return "MobGen{" +
                "mob=" + (mob == null ? -1 : mob.getTemplateId()) +
                '}';
    }
}
