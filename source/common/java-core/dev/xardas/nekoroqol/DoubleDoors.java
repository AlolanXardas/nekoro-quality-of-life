package dev.xardas.nekoroqol;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Opening one door of a double door also opens the other. Runs on both sides, so the second door
 * moves at once on the client too. Only doors that open by hand; sneaking opens just the one door.
 */
public final class DoubleDoors {
    private DoubleDoors() { }

    /** Right before a player uses the block at pos. */
    public static void onUseBlock(Player player, Level level, InteractionHand hand, BlockPos pos) {
        if (!QolConfig.get().doubleDoors || hand != InteractionHand.MAIN_HAND) return;
        if (player.isSecondaryUseActive()) return;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock door) || !door.type().canOpenByHand()) return;

        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
        // the partner stands on the side away from the hinge, faces the same way and has its hinge on the other side
        Direction toPartner = hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos partnerPos = pos.relative(toPartner);
        BlockState partner = level.getBlockState(partnerPos);
        if (!(partner.getBlock() instanceof DoorBlock partnerDoor) || !partnerDoor.type().canOpenByHand()) return;
        if (partner.getValue(DoorBlock.FACING) != facing || partner.getValue(DoorBlock.HINGE) == hinge
                || partner.getValue(DoorBlock.HALF) != state.getValue(DoorBlock.HALF)) return;

        boolean open = state.getValue(DoorBlock.OPEN);
        // the clicked door has not moved yet, it changes right after this event
        if (partner.getValue(DoorBlock.OPEN) == open) {
            BlockPos lower = partner.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? partnerPos : partnerPos.below();
            partnerDoor.setOpen(player, level, level.getBlockState(lower), lower, !open);
        }
    }
}
