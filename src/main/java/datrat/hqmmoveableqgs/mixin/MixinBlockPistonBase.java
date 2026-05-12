package datrat.hqmmoveableqgs.mixin;

import datrat.hqmmoveableqgs.piston.HqmPortalHooks;
import datrat.hqmmoveableqgs.piston.HqmPortalSnapshot;
import datrat.hqmmoveableqgs.piston.IHqmPortalPistonData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityPiston;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPistonBase.class)
public abstract class MixinBlockPistonBase {
    @Inject(method = "canPushBlock(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;IIIZ)Z", at = @At("HEAD"), cancellable = true)
    private static void hqmmoveableqgs$allowPortalPush(Block block, World world, int x, int y, int z, boolean allowDestroy, CallbackInfoReturnable<Boolean> cir) {
        if (HqmPortalHooks.isHqmPortal(world, x, y, z, block)) {
            HqmPortalHooks.debugPortalCheck("canPushBlock", world, x, y, z, block, true);
            cir.setReturnValue(Boolean.valueOf(hqmmoveableqgs$canPushBlock(block, world, x, y, z, allowDestroy)));
        }
    }

    @Inject(method = "canExtend(Lnet/minecraft/world/World;IIII)Z", at = @At("HEAD"), cancellable = true)
    private static void hqmmoveableqgs$canExtendPortalLine(World world, int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.valueOf(hqmmoveableqgs$canExtendLine(world, x, y, z, direction)));
    }

    @Inject(method = "onBlockEventReceived(Lnet/minecraft/world/World;IIIII)Z", at = @At("HEAD"), cancellable = true)
    private void hqmmoveableqgs$extendWithPortal(World world, int x, int y, int z, int eventId, int direction, CallbackInfoReturnable<Boolean> cir) {
        if (eventId != 0) {
            return;
        }

        if (!hqmmoveableqgs$moveWithPortalData(world, x, y, z, direction)) {
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        world.setBlockMetadataWithNotify(x, y, z, direction | 8, 2);
        world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "tile.piston.out", 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
        cir.setReturnValue(Boolean.TRUE);
    }

    @Inject(method = "tryExtend(Lnet/minecraft/world/World;IIII)Z", at = @At("HEAD"), cancellable = true)
    private void hqmmoveableqgs$tryExtendWithPortal(World world, int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
        if (hqmmoveableqgs$extensionTouchesPortal(world, x, y, z, direction)) {
            cir.setReturnValue(Boolean.valueOf(hqmmoveableqgs$moveWithPortalData(world, x, y, z, direction)));
        }
    }

    @Inject(method = "onBlockEventReceived(Lnet/minecraft/world/World;IIIII)Z", at = @At("HEAD"), cancellable = true)
    private void hqmmoveableqgs$stickyRetractWithPortal(World world, int x, int y, int z, int eventId, int direction, CallbackInfoReturnable<Boolean> cir) {
        if (eventId != 1 || !hqmmoveableqgs$isStickyPiston() || !hqmmoveableqgs$retractionTouchesPortal(world, x, y, z, direction)) {
            return;
        }

        if (!world.isRemote && hqmmoveableqgs$isIndirectlyPowered(world, x, y, z, direction)) {
            world.setBlockMetadataWithNotify(x, y, z, direction | 8, 2);
            cir.setReturnValue(Boolean.FALSE);
            return;
        }

        cir.setReturnValue(Boolean.valueOf(hqmmoveableqgs$handleStickyRetractionWithPortal(world, x, y, z, direction)));
    }

    @Unique
    private static boolean hqmmoveableqgs$canPushBlock(Block block, World world, int x, int y, int z, boolean allowDestroy) {
        if (block == Blocks.obsidian) {
            return false;
        }

        if (HqmPortalHooks.isHqmPortal(world, x, y, z, block)) {
            return true;
        }

        if (block != Blocks.piston && block != Blocks.sticky_piston) {
            if (block.getBlockHardness(world, x, y, z) == -1.0F) {
                return false;
            }

            if (block.getMobilityFlag() == 2) {
                return false;
            }

            if (block.getMobilityFlag() == 1) {
                return allowDestroy;
            }
        } else if ((world.getBlockMetadata(x, y, z) & 8) != 0) {
            return false;
        }

        return !(block instanceof ITileEntityProvider);
    }

    @Unique
    private boolean hqmmoveableqgs$extensionTouchesPortal(World world, int x, int y, int z, int direction) {
        return hqmmoveableqgs$extensionTouchesPortalStatic(world, x, y, z, direction);
    }

    @Unique
    private static boolean hqmmoveableqgs$canExtendLine(World world, int x, int y, int z, int direction) {
        int moveX = x + Facing.offsetsXForSide[direction];
        int moveY = y + Facing.offsetsYForSide[direction];
        int moveZ = z + Facing.offsetsZForSide[direction];

        for (int moved = 0; moved < 13; moved++) {
            if (moveY <= 0 || moveY >= world.getHeight()) {
                return false;
            }

            Block block = world.getBlock(moveX, moveY, moveZ);
            if (block.isAir(world, moveX, moveY, moveZ)) {
                return true;
            }

            if (!hqmmoveableqgs$canPushBlock(block, world, moveX, moveY, moveZ, true)) {
                return false;
            }

            if (hqmmoveableqgs$isDestroyOnPushBlock(block, world, moveX, moveY, moveZ)) {
                return true;
            }

            if (moved == 12) {
                return false;
            }

            moveX += Facing.offsetsXForSide[direction];
            moveY += Facing.offsetsYForSide[direction];
            moveZ += Facing.offsetsZForSide[direction];
        }

        return true;
    }

    @Unique
    private static boolean hqmmoveableqgs$extensionTouchesPortalStatic(World world, int x, int y, int z, int direction) {
        int moveX = x + Facing.offsetsXForSide[direction];
        int moveY = y + Facing.offsetsYForSide[direction];
        int moveZ = z + Facing.offsetsZForSide[direction];
        boolean foundPortal = false;

        for (int moved = 0; moved < 13; moved++) {
            if (moveY <= 0 || moveY >= world.getHeight()) {
                return false;
            }

            Block block = world.getBlock(moveX, moveY, moveZ);
            if (block.isAir(world, moveX, moveY, moveZ)) {
                return foundPortal;
            }

            if (!hqmmoveableqgs$canPushBlock(block, world, moveX, moveY, moveZ, true)) {
                return false;
            }

            boolean portal = HqmPortalHooks.isHqmPortal(world, moveX, moveY, moveZ, block);
            if (portal) {
                HqmPortalHooks.debugPortalCheck("extension scan", world, moveX, moveY, moveZ, block, true);
            }
            foundPortal |= portal;
            if (hqmmoveableqgs$isDestroyOnPushBlock(block, world, moveX, moveY, moveZ)) {
                return foundPortal;
            }

            if (moved == 12) {
                return false;
            }

            moveX += Facing.offsetsXForSide[direction];
            moveY += Facing.offsetsYForSide[direction];
            moveZ += Facing.offsetsZForSide[direction];
        }

        return false;
    }

    @Unique
    private static boolean hqmmoveableqgs$isDestroyOnPushBlock(Block block, World world, int x, int y, int z) {
        return block.getMobilityFlag() == 1 && !HqmPortalHooks.isHqmPortal(world, x, y, z, block);
    }

    @Unique
    private boolean hqmmoveableqgs$moveWithPortalData(World world, int x, int y, int z, int direction) {
        int moveX = x + Facing.offsetsXForSide[direction];
        int moveY = y + Facing.offsetsYForSide[direction];
        int moveZ = z + Facing.offsetsZForSide[direction];
        int moved = 0;

        while (true) {
            if (moved < 13) {
                if (moveY <= 0 || moveY >= 255) {
                    return false;
                }

                Block block = world.getBlock(moveX, moveY, moveZ);
                if (!block.isAir(world, moveX, moveY, moveZ)) {
                    if (!hqmmoveableqgs$canPushBlock(block, world, moveX, moveY, moveZ, true)) {
                        return false;
                    }

                    if (!hqmmoveableqgs$isDestroyOnPushBlock(block, world, moveX, moveY, moveZ)) {
                        if (moved == 12) {
                            return false;
                        }

                        moveX += Facing.offsetsXForSide[direction];
                        moveY += Facing.offsetsYForSide[direction];
                        moveZ += Facing.offsetsZForSide[direction];
                        ++moved;
                        continue;
                    }

                    block.dropBlockAsItem(world, moveX, moveY, moveZ, world.getBlockMetadata(moveX, moveY, moveZ), 0);
                    world.setBlockToAir(moveX, moveY, moveZ);
                }
            }

            int notifyX = moveX;
            int notifyY = moveY;
            int notifyZ = moveZ;
            int movedCount = 0;
            Block[] movedBlocks = new Block[13];

            while (moveX != x || moveY != y || moveZ != z) {
                int sourceX = moveX - Facing.offsetsXForSide[direction];
                int sourceY = moveY - Facing.offsetsYForSide[direction];
                int sourceZ = moveZ - Facing.offsetsZForSide[direction];
                Block sourceBlock = world.getBlock(sourceX, sourceY, sourceZ);
                int sourceMeta = world.getBlockMetadata(sourceX, sourceY, sourceZ);

                if (sourceBlock == (Block) (Object) this && sourceX == x && sourceY == y && sourceZ == z) {
                    int pistonHeadMeta = direction | (hqmmoveableqgs$isStickyPiston() ? 8 : 0);
                    hqmmoveableqgs$setMovingBlock(world, moveX, moveY, moveZ, Blocks.piston_head, pistonHeadMeta, direction, true, false, 4, null);
                } else {
                    HqmPortalSnapshot portalSnapshot = HqmPortalHooks.isHqmPortal(world, sourceX, sourceY, sourceZ, sourceBlock) ? HqmPortalHooks.capturePortalSnapshot(world, sourceX, sourceY, sourceZ) : null;
                    hqmmoveableqgs$setMovingBlock(world, moveX, moveY, moveZ, sourceBlock, sourceMeta, direction, true, false, 4, portalSnapshot);
                }

                movedBlocks[movedCount++] = sourceBlock;
                moveX = sourceX;
                moveY = sourceY;
                moveZ = sourceZ;
            }

            moveX = notifyX;
            moveY = notifyY;
            moveZ = notifyZ;

            for (movedCount = 0; moveX != x || moveY != y || moveZ != z; moveZ = notifyZ) {
                notifyX = moveX - Facing.offsetsXForSide[direction];
                notifyY = moveY - Facing.offsetsYForSide[direction];
                notifyZ = moveZ - Facing.offsetsZForSide[direction];
                world.notifyBlocksOfNeighborChange(notifyX, notifyY, notifyZ, movedBlocks[movedCount++]);
                moveX = notifyX;
                moveY = notifyY;
            }

            return true;
        }
    }

    @Unique
    private boolean hqmmoveableqgs$retractionTouchesPortal(World world, int x, int y, int z, int direction) {
        int targetX = x + Facing.offsetsXForSide[direction] * 2;
        int targetY = y + Facing.offsetsYForSide[direction] * 2;
        int targetZ = z + Facing.offsetsZForSide[direction] * 2;
        Block targetBlock = world.getBlock(targetX, targetY, targetZ);

        if (HqmPortalHooks.isHqmPortal(world, targetX, targetY, targetZ, targetBlock)) {
            HqmPortalHooks.debugPortalCheck("sticky retract target", world, targetX, targetY, targetZ, targetBlock, true);
            return true;
        }

        if (targetBlock != Blocks.piston_extension) {
            return false;
        }

        TileEntity tileEntity = world.getTileEntity(targetX, targetY, targetZ);
        if (!(tileEntity instanceof TileEntityPiston)) {
            return false;
        }

        TileEntityPiston pistonTile = (TileEntityPiston) tileEntity;
        return HqmPortalHooks.isHqmPortal(pistonTile.getStoredBlockID()) || tileEntity instanceof IHqmPortalPistonData && ((IHqmPortalPistonData) tileEntity).hqmmoveableqgs$getPortalSnapshot() != null;
    }

    @Unique
    private boolean hqmmoveableqgs$handleStickyRetractionWithPortal(World world, int x, int y, int z, int direction) {
        TileEntity tileEntity = world.getTileEntity(x + Facing.offsetsXForSide[direction], y + Facing.offsetsYForSide[direction], z + Facing.offsetsZForSide[direction]);
        if (tileEntity instanceof TileEntityPiston) {
            ((TileEntityPiston) tileEntity).clearPistonTileEntity();
        }

        hqmmoveableqgs$setMovingBlock(world, x, y, z, (Block) (Object) this, direction, direction, false, true, 3, null);

        int targetX = x + Facing.offsetsXForSide[direction] * 2;
        int targetY = y + Facing.offsetsYForSide[direction] * 2;
        int targetZ = z + Facing.offsetsZForSide[direction] * 2;
        Block pulledBlock = world.getBlock(targetX, targetY, targetZ);
        int pulledMeta = world.getBlockMetadata(targetX, targetY, targetZ);
        boolean alreadyMoving = false;

        if (pulledBlock == Blocks.piston_extension) {
            TileEntity pulledTileEntity = world.getTileEntity(targetX, targetY, targetZ);
            if (pulledTileEntity instanceof TileEntityPiston) {
                TileEntityPiston pulledPiston = (TileEntityPiston) pulledTileEntity;
                if (pulledPiston.getPistonOrientation() == direction && pulledPiston.isExtending()) {
                    pulledPiston.clearPistonTileEntity();
                    pulledBlock = pulledPiston.getStoredBlockID();
                    pulledMeta = pulledPiston.getBlockMetadata();
                    alreadyMoving = true;
                }
            }
        }

        boolean pulledPortal = HqmPortalHooks.isHqmPortal(world, targetX, targetY, targetZ, pulledBlock);
        if (!alreadyMoving && !pulledBlock.isAir(world, targetX, targetY, targetZ) && hqmmoveableqgs$canPushBlock(pulledBlock, world, targetX, targetY, targetZ, false) && (pulledPortal || pulledBlock.getMobilityFlag() == 0 || pulledBlock == Blocks.piston || pulledBlock == Blocks.sticky_piston)) {
            HqmPortalSnapshot portalSnapshot = pulledPortal ? HqmPortalHooks.capturePortalSnapshot(world, targetX, targetY, targetZ) : null;
            x += Facing.offsetsXForSide[direction];
            y += Facing.offsetsYForSide[direction];
            z += Facing.offsetsZForSide[direction];
            hqmmoveableqgs$setMovingBlock(world, x, y, z, pulledBlock, pulledMeta, direction, false, false, 3, portalSnapshot);
            world.setBlockToAir(targetX, targetY, targetZ);
        } else if (!alreadyMoving) {
            world.setBlockToAir(x + Facing.offsetsXForSide[direction], y + Facing.offsetsYForSide[direction], z + Facing.offsetsZForSide[direction]);
        }

        world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "tile.piston.in", 0.5F, world.rand.nextFloat() * 0.15F + 0.6F);
        return true;
    }

    @Unique
    private void hqmmoveableqgs$setMovingBlock(World world, int x, int y, int z, Block movedBlock, int movedMeta, int direction, boolean extending, boolean source, int flags, HqmPortalSnapshot portalSnapshot) {
        world.setBlock(x, y, z, Blocks.piston_extension, movedMeta, flags);
        TileEntity movingTile = BlockPistonMoving.getTileEntity(movedBlock, movedMeta, direction, extending, source);
        if (portalSnapshot != null) {
            HqmPortalHooks.rememberMovingPortal(world, x, y, z, portalSnapshot);
            if (movingTile instanceof IHqmPortalPistonData) {
                ((IHqmPortalPistonData) movingTile).hqmmoveableqgs$setPortalSnapshot(portalSnapshot);
            }
        }
        world.setTileEntity(x, y, z, movingTile);
    }

    @Unique
    private boolean hqmmoveableqgs$isStickyPiston() {
        return (Block) (Object) this == Blocks.sticky_piston;
    }

    @Unique
    private static boolean hqmmoveableqgs$isIndirectlyPowered(World world, int x, int y, int z, int direction) {
        return direction != 0 && world.getIndirectPowerOutput(x, y - 1, z, 0)
            || direction != 1 && world.getIndirectPowerOutput(x, y + 1, z, 1)
            || direction != 2 && world.getIndirectPowerOutput(x, y, z - 1, 2)
            || direction != 3 && world.getIndirectPowerOutput(x, y, z + 1, 3)
            || direction != 5 && world.getIndirectPowerOutput(x + 1, y, z, 5)
            || direction != 4 && world.getIndirectPowerOutput(x - 1, y, z, 4)
            || world.getIndirectPowerOutput(x, y, z, 0)
            || world.getIndirectPowerOutput(x, y + 2, z, 1)
            || world.getIndirectPowerOutput(x, y + 1, z - 1, 2)
            || world.getIndirectPowerOutput(x, y + 1, z + 1, 3)
            || world.getIndirectPowerOutput(x - 1, y + 1, z, 4)
            || world.getIndirectPowerOutput(x + 1, y + 1, z, 5);
    }
}
