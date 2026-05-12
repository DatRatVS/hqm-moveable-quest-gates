package datrat.hqmmoveableqgs.piston;

import hardcorequesting.tileentity.PortalType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HqmPortalSnapshot {
    private static final String KEY_QUEST = "Quest";
    private static final String KEY_TYPE = "PortalType";
    private static final String KEY_ITEM_ID = "ItemId";
    private static final String KEY_ITEM_DMG = "ItemDmg";
    private static final String KEY_COLLISION = "Collision";
    private static final String KEY_TEXTURES = "Textures";
    private static final String KEY_NOT_COLLISION = "NotCollision";
    private static final String KEY_NOT_TEXTURES = "NotTextures";
    private static final String KEY_PLAYERS = "hqmmoveableqgs:Players";

    private final int questId;
    private final PortalType type;
    private final ItemStack item;
    private final boolean completedTexture;
    private final boolean uncompletedTexture;
    private final boolean completedCollision;
    private final boolean uncompletedCollision;
    private final List<String> players;

    public HqmPortalSnapshot(int questId, PortalType type, ItemStack item, boolean completedTexture, boolean uncompletedTexture, boolean completedCollision, boolean uncompletedCollision, List<String> players) {
        this.questId = questId;
        this.type = type == null ? PortalType.TECH : type;
        this.item = item == null ? null : item.copy();
        this.completedTexture = completedTexture;
        this.uncompletedTexture = uncompletedTexture;
        this.completedCollision = completedCollision;
        this.uncompletedCollision = uncompletedCollision;
        this.players = players == null ? Collections.<String>emptyList() : new ArrayList<String>(players);
    }

    public int getQuestId() {
        return this.questId;
    }

    public PortalType getType() {
        return this.type;
    }

    public ItemStack getItem() {
        return this.item == null ? null : this.item.copy();
    }

    public boolean isCompletedTexture() {
        return this.completedTexture;
    }

    public boolean isUncompletedTexture() {
        return this.uncompletedTexture;
    }

    public boolean isCompletedCollision() {
        return this.completedCollision;
    }

    public boolean isUncompletedCollision() {
        return this.uncompletedCollision;
    }

    public List<String> getPlayers() {
        return new ArrayList<String>(this.players);
    }

    public String describe() {
        return "quest=" + this.questId
            + ", type=" + this.type
            + ", item=" + (this.item == null ? "<none>" : this.item.toString())
            + ", completedTexture=" + this.completedTexture
            + ", uncompletedTexture=" + this.uncompletedTexture
            + ", completedCollision=" + this.completedCollision
            + ", uncompletedCollision=" + this.uncompletedCollision
            + ", players=" + this.players.size();
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.questId >= 0) {
            tag.setShort(KEY_QUEST, (short) this.questId);
        }
        tag.setByte(KEY_TYPE, (byte) this.type.ordinal());
        if (this.item != null) {
            tag.setShort(KEY_ITEM_ID, (short) Item.getIdFromItem(this.item.getItem()));
            tag.setShort(KEY_ITEM_DMG, (short) this.item.getMetadata());
        }
        tag.setBoolean(KEY_COLLISION, this.completedCollision);
        tag.setBoolean(KEY_TEXTURES, this.completedTexture);
        tag.setBoolean(KEY_NOT_COLLISION, this.uncompletedCollision);
        tag.setBoolean(KEY_NOT_TEXTURES, this.uncompletedTexture);

        NBTTagList playerTags = new NBTTagList();
        for (String player : this.players) {
            playerTags.appendTag(new NBTTagString(player));
        }
        tag.setTag(KEY_PLAYERS, playerTags);
        return tag;
    }

    public static HqmPortalSnapshot fromNbt(NBTTagCompound tag) {
        int questId = tag.hasKey(KEY_QUEST) ? tag.getShort(KEY_QUEST) : -1;
        PortalType[] values = PortalType.values();
        int typeId = tag.getByte(KEY_TYPE);
        PortalType type = typeId >= 0 && typeId < values.length ? values[typeId] : PortalType.TECH;
        ItemStack item = null;
        if (tag.hasKey(KEY_ITEM_ID)) {
            item = new ItemStack(Item.getItemById(tag.getShort(KEY_ITEM_ID)), 1, tag.getShort(KEY_ITEM_DMG));
        }

        List<String> players = new ArrayList<String>();
        NBTTagList playerTags = tag.getTagList(KEY_PLAYERS, 8);
        for (int i = 0; i < playerTags.tagCount(); i++) {
            players.add(playerTags.getStringTagAt(i));
        }

        return new HqmPortalSnapshot(
            questId,
            type,
            item,
            tag.getBoolean(KEY_TEXTURES),
            !tag.hasKey(KEY_NOT_TEXTURES) || tag.getBoolean(KEY_NOT_TEXTURES),
            tag.getBoolean(KEY_COLLISION),
            !tag.hasKey(KEY_NOT_COLLISION) || tag.getBoolean(KEY_NOT_COLLISION),
            players
        );
    }
}
