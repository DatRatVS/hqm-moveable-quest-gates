package datrat.hqmmoveableqgs.mixin;

import datrat.hqmmoveableqgs.piston.HqmPortalHooks;
import datrat.hqmmoveableqgs.piston.HqmPortalSnapshot;
import datrat.hqmmoveableqgs.piston.IHqmPortalPistonData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityPiston;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityPiston.class)
public abstract class MixinTileEntityPiston implements IHqmPortalPistonData {
    @Unique
    private static final String HQMMOVEABLEQGS_PORTAL_SNAPSHOT = "hqmmoveableqgs:PortalSnapshot";

    @Unique
    private HqmPortalSnapshot hqmmoveableqgs$portalSnapshot;

    @Override
    public void hqmmoveableqgs$setPortalSnapshot(HqmPortalSnapshot snapshot) {
        this.hqmmoveableqgs$portalSnapshot = snapshot;
    }

    @Override
    public HqmPortalSnapshot hqmmoveableqgs$getPortalSnapshot() {
        return this.hqmmoveableqgs$portalSnapshot;
    }

    @Inject(method = "writeToNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    private void hqmmoveableqgs$writePortalData(NBTTagCompound tag, CallbackInfo ci) {
        if (this.hqmmoveableqgs$portalSnapshot != null) {
            tag.setTag(HQMMOVEABLEQGS_PORTAL_SNAPSHOT, this.hqmmoveableqgs$portalSnapshot.toNbt());
        }
    }

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("TAIL"))
    private void hqmmoveableqgs$readPortalData(NBTTagCompound tag, CallbackInfo ci) {
        if (tag.hasKey(HQMMOVEABLEQGS_PORTAL_SNAPSHOT)) {
            this.hqmmoveableqgs$portalSnapshot = HqmPortalSnapshot.fromNbt(tag.getCompoundTag(HQMMOVEABLEQGS_PORTAL_SNAPSHOT));
        }
    }

    @Inject(method = "clearPistonTileEntity()V", at = @At("TAIL"))
    private void hqmmoveableqgs$restoreAfterClear(CallbackInfo ci) {
        this.hqmmoveableqgs$restorePortalData();
    }

    @Inject(method = "updateEntity()V", at = @At("TAIL"))
    private void hqmmoveableqgs$restoreAfterUpdate(CallbackInfo ci) {
        this.hqmmoveableqgs$restorePortalData();
    }

    @Unique
    private void hqmmoveableqgs$restorePortalData() {
        TileEntityPiston self = (TileEntityPiston) (Object) this;
        if (this.hqmmoveableqgs$portalSnapshot == null || self.getWorld() == null) {
            return;
        }

        HqmPortalHooks.restoreMovingPortalIfReady(self.getWorld(), self.xCoord, self.yCoord, self.zCoord, this.hqmmoveableqgs$portalSnapshot);
    }
}
