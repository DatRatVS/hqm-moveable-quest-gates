package datrat.hqmmoveableqgs.piston;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import datrat.hqmmoveableqgs.HQMMoveableQgs;
import hardcorequesting.blocks.BlockPortal;
import hardcorequesting.network.PacketHandler;
import hardcorequesting.quests.Quest;
import hardcorequesting.tileentity.IBlockSync;
import hardcorequesting.tileentity.TileEntityPortal;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HqmPortalHooks {
    private static final String HQM_PORTAL_REGISTRY_NAME = "hqm:quest_portal";
    private static final String HQM_PORTAL_FORGE_REGISTRY_NAME = "HardcoreQuesting:quest_portal";
    private static final String HQM_PORTAL_CLASS_NAME = "hardcorequesting.blocks.BlockPortal";
    private static final String HQM_PORTAL_TILE_CLASS_NAME = "hardcorequesting.tileentity.TileEntityPortal";
    private static final String HQM_PORTAL_QUEST_KEY = "Quest";

    private static Field questField;
    private static Field questIdField;
    private static Field playersField;
    private static boolean pendingRestoreTickerRegistered;
    private static final Map<String, PendingPortalRestore> PENDING_RESTORES = new LinkedHashMap<String, PendingPortalRestore>();

    private HqmPortalHooks() {
    }

    public static void registerPendingRestoreTicker() {
        if (!pendingRestoreTickerRegistered) {
            pendingRestoreTickerRegistered = true;
            FMLCommonHandler.instance().bus().register(new PendingRestoreTicker());
        }
    }

    public static boolean isHqmPortal(Block block) {
        if (block == null) {
            return false;
        }

        if (block instanceof BlockPortal) {
            return true;
        }

        String name = Block.blockRegistry.getNameForObject(block);
        return isPortalRegistryName(name) || HQM_PORTAL_CLASS_NAME.equals(block.getClass().getName());
    }

    public static boolean isHqmPortal(World world, int x, int y, int z, Block block) {
        if (isHqmPortal(block)) {
            return true;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        return tileEntity instanceof TileEntityPortal || tileEntity != null && HQM_PORTAL_TILE_CLASS_NAME.equals(tileEntity.getClass().getName());
    }

    public static void debugPortalCheck(String context, World world, int x, int y, int z, Block block, boolean portal) {
        if (HQMMoveableQgs.logger == null) {
            return;
        }

        String name = block == null ? "<null>" : String.valueOf(Block.blockRegistry.getNameForObject(block));
        String className = block == null ? "<null>" : block.getClass().getName();
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        String tileClassName = tileEntity == null ? "<none>" : tileEntity.getClass().getName();
        HQMMoveableQgs.logger.debug("{} at {},{},{} block={} class={} tile={} portal={}", context, Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), name, className, tileClassName, Boolean.valueOf(portal));
    }

    private static boolean isPortalRegistryName(String name) {
        if (name == null) {
            return false;
        }

        return HQM_PORTAL_REGISTRY_NAME.equals(name)
            || HQM_PORTAL_FORGE_REGISTRY_NAME.equals(name)
            || "hardcorequesting:quest_portal".equalsIgnoreCase(name)
            || name.toLowerCase().endsWith(":quest_portal");
    }

    public static NBTTagCompound capturePortalNbt(World world, int x, int y, int z) {
        HqmPortalSnapshot snapshot = capturePortalSnapshot(world, x, y, z);
        return snapshot == null ? null : snapshot.toNbt();
    }

    public static HqmPortalSnapshot capturePortalSnapshot(World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityPortal)) {
            return null;
        }

        TileEntityPortal portal = (TileEntityPortal) tileEntity;
        NBTTagCompound tag = new NBTTagCompound();
        portal.writeContentToNBT(tag);
        writeQuestIdIfMissing(portal, tag);
        int questId = tag.hasKey(HQM_PORTAL_QUEST_KEY) ? tag.getShort(HQM_PORTAL_QUEST_KEY) : getPendingQuestId(portal);
        ItemStack item = portal.getItem();
        HqmPortalSnapshot snapshot = new HqmPortalSnapshot(
            questId,
            portal.getType(),
            item == null ? null : item.copy(),
            portal.isCompletedTexture(),
            portal.isUncompletedTexture(),
            portal.isCompletedCollision(),
            portal.isUncompletedCollision(),
            getPlayers(portal)
        );
        if (HQMMoveableQgs.logger != null) {
            HQMMoveableQgs.logger.info("Captured HQM portal snapshot at {},{},{}: {}", Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), snapshot.describe());
        }
        return snapshot;
    }

    public static void applyPortalNbt(World world, int x, int y, int z, NBTTagCompound tag) {
        applyPortalSnapshot(world, x, y, z, tag == null ? null : HqmPortalSnapshot.fromNbt(tag));
    }

    public static void applyPortalSnapshot(World world, int x, int y, int z, HqmPortalSnapshot snapshot) {
        if (snapshot == null || !isHqmPortal(world.getBlock(x, y, z))) {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityPortal)) {
            world.setTileEntity(x, y, z, new TileEntityPortal());
            tileEntity = world.getTileEntity(x, y, z);
            if (!(tileEntity instanceof TileEntityPortal)) {
                return;
            }
        }

        TileEntityPortal portal = (TileEntityPortal) tileEntity;
        portal.readContentFromNBT(snapshot.toNbt());
        restorePrivatePortalState(portal, snapshot);
        tileEntity.markDirty();
        world.markBlockForUpdate(x, y, z);
        if (HQMMoveableQgs.logger != null) {
            HQMMoveableQgs.logger.info("Restored HQM portal snapshot at {},{},{}: {}", Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), snapshot.describe());
        }
        if (!world.isRemote && tileEntity instanceof IBlockSync) {
            PacketHandler.sendBlockPacket((IBlockSync) tileEntity, null, 0);
        }
    }

    public static void rememberMovingPortal(World world, int x, int y, int z, HqmPortalSnapshot snapshot) {
        if (world == null || snapshot == null) {
            return;
        }

        PendingPortalRestore pending = new PendingPortalRestore(world.provider.dimensionId, x, y, z, snapshot);
        PENDING_RESTORES.put(pending.key(), pending);
        if (HQMMoveableQgs.logger != null) {
            HQMMoveableQgs.logger.info("Queued HQM portal restore at {},{},{}: {}", Integer.valueOf(x), Integer.valueOf(y), Integer.valueOf(z), snapshot.describe());
        }
    }

    public static void restoreMovingPortalIfReady(World world, int x, int y, int z, HqmPortalSnapshot fallbackSnapshot) {
        HqmPortalSnapshot snapshot = fallbackSnapshot;
        String key = key(world, x, y, z);
        PendingPortalRestore pending = key == null ? null : PENDING_RESTORES.get(key);
        if (snapshot == null && pending != null) {
            snapshot = pending.snapshot;
        }

        if (snapshot == null || !isHqmPortal(world.getBlock(x, y, z))) {
            return;
        }

        applyPortalSnapshot(world, x, y, z, snapshot);
        if (key != null) {
            PENDING_RESTORES.remove(key);
        }
    }

    private static void writeQuestIdIfMissing(TileEntityPortal portal, NBTTagCompound tag) {
        if (tag.hasKey(HQM_PORTAL_QUEST_KEY)) {
            return;
        }

        Quest quest = portal.getCurrentQuest();
        if (quest != null) {
            tag.setShort(HQM_PORTAL_QUEST_KEY, quest.getId());
            return;
        }

        int questId = getPendingQuestId(portal);
        if (questId >= 0) {
            tag.setShort(HQM_PORTAL_QUEST_KEY, (short) questId);
        }
    }

    private static int getPendingQuestId(TileEntityPortal portal) {
        try {
            Field field = getQuestIdField();
            return field == null ? -1 : field.getInt(portal);
        } catch (IllegalAccessException ignored) {
            return -1;
        }
    }

    private static Field getQuestIdField() {
        if (questIdField == null) {
            try {
                questIdField = TileEntityPortal.class.getDeclaredField("questId");
                questIdField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                return null;
            }
        }

        return questIdField;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getPlayers(TileEntityPortal portal) {
        try {
            Field field = getPlayersField();
            if (field == null) {
                return Collections.emptyList();
            }

            Object value = field.get(portal);
            return value instanceof List ? new ArrayList<String>((List<String>) value) : Collections.<String>emptyList();
        } catch (IllegalAccessException ignored) {
            return Collections.emptyList();
        }
    }

    private static void restorePrivatePortalState(TileEntityPortal portal, HqmPortalSnapshot snapshot) {
        portal.setType(snapshot.getType());
        portal.setItem(snapshot.getItem());
        portal.setCompletedTexture(snapshot.isCompletedTexture());
        portal.setUncompletedTexture(snapshot.isUncompletedTexture());
        portal.setCompletedCollision(snapshot.isCompletedCollision());
        portal.setUncompletedCollision(snapshot.isUncompletedCollision());
        setPlayers(portal, snapshot.getPlayers());
        setQuest(portal, snapshot.getQuestId());
    }

    private static void setQuest(TileEntityPortal portal, int questId) {
        try {
            Quest resolvedQuest = questId < 0 ? null : Quest.getQuest(questId);
            Field questObjectField = getQuestField();
            if (questObjectField != null) {
                questObjectField.set(portal, resolvedQuest);
            }

            Field pendingQuestId = getQuestIdField();
            if (pendingQuestId != null) {
                pendingQuestId.setInt(portal, resolvedQuest == null ? questId : -1);
            }
        } catch (Throwable ignored) {
            try {
                Field pendingQuestId = getQuestIdField();
                if (pendingQuestId != null) {
                    pendingQuestId.setInt(portal, questId);
                }
            } catch (IllegalAccessException ignoredToo) {
            }
        }
    }

    private static void setPlayers(TileEntityPortal portal, List<String> players) {
        try {
            Field field = getPlayersField();
            if (field != null) {
                field.set(portal, new ArrayList<String>(players));
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field getQuestField() {
        if (questField == null) {
            questField = findPortalField("quest");
        }

        return questField;
    }

    private static Field getPlayersField() {
        if (playersField == null) {
            playersField = findPortalField("players");
        }

        return playersField;
    }

    private static Field findPortalField(String name) {
        try {
            Field field = TileEntityPortal.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    private static String key(World world, int x, int y, int z) {
        return world == null || world.provider == null ? null : world.provider.dimensionId + ":" + x + ":" + y + ":" + z;
    }

    private static final class PendingPortalRestore {
        private final int dimension;
        private final int x;
        private final int y;
        private final int z;
        private final HqmPortalSnapshot snapshot;
        private int ticks;

        private PendingPortalRestore(int dimension, int x, int y, int z, HqmPortalSnapshot snapshot) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.snapshot = snapshot;
        }

        private String key() {
            return this.dimension + ":" + this.x + ":" + this.y + ":" + this.z;
        }
    }

    public static final class PendingRestoreTicker {
        @SubscribeEvent
        public void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world == null || event.world.isRemote || PENDING_RESTORES.isEmpty()) {
                return;
            }

            int dimension = event.world.provider.dimensionId;
            Iterator<Map.Entry<String, PendingPortalRestore>> iterator = PENDING_RESTORES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, PendingPortalRestore> entry = iterator.next();
                PendingPortalRestore pending = entry.getValue();
                if (pending.dimension != dimension) {
                    continue;
                }

                if (isHqmPortal(event.world.getBlock(pending.x, pending.y, pending.z))) {
                    applyPortalSnapshot(event.world, pending.x, pending.y, pending.z, pending.snapshot);
                    iterator.remove();
                } else if (++pending.ticks > 100) {
                    if (HQMMoveableQgs.logger != null) {
                        HQMMoveableQgs.logger.warn("Dropping stale HQM portal restore at {},{},{} after {} ticks", Integer.valueOf(pending.x), Integer.valueOf(pending.y), Integer.valueOf(pending.z), Integer.valueOf(pending.ticks));
                    }
                    iterator.remove();
                }
            }
        }
    }
}
