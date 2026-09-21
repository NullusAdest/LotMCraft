package main.java.de.jakob.lotm.network.packets.toServer;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactItem;
import de.jakob.lotm.data.ModDataComponents;
import de.jakob.lotm.gui.custom.artifact_wheel.ArtifactWheelMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

//Sealed artifact Rework:
import net.minecraft.world.entity.EquipmentSlot;
//Sealed artifact Rework:

public record UseArtifactAbilityPacket() implements CustomPacketPayload {
    public static final Type<UseArtifactAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "use_artifact_ability"));
    public static final StreamCodec<FriendlyByteBuf, UseArtifactAbilityPacket> STREAM_CODEC =
            StreamCodec.unit(new UseArtifactAbilityPacket());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //Sealed artifact Rework:
    public static void handle(UseArtifactAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            EquipmentSlot activeSlot = null;
            ItemStack activeStack = ItemStack.EMPTY;
            for (EquipmentSlot slot : ArtifactWheelMenu.SEALED_ARTIFACT_SLOTS) {
                ItemStack equipped = serverPlayer.getItemBySlot(slot);
                if (equipped.has(ModDataComponents.SEALED_ARTIFACT_DATA)
                        && equipped.getOrDefault(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, false)) {
                    activeSlot = slot;
                    activeStack = equipped;
                    break;
                }
            }
            if (activeSlot == null) {
                ItemStack main = serverPlayer.getItemBySlot(EquipmentSlot.MAINHAND);
                if (main.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                    activeSlot = EquipmentSlot.MAINHAND;
                    activeStack = main;
                } else {
                    ItemStack off = serverPlayer.getItemBySlot(EquipmentSlot.OFFHAND);
                    if (off.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                        activeSlot = EquipmentSlot.OFFHAND;
                        activeStack = off;
                    }
                }
            }
            if (activeSlot == null) return;
            SealedArtifactItem.tryUseArtifactAbility((ServerLevel) serverPlayer.level(), serverPlayer, activeSlot, activeStack);
        });
    }
    //Sealed artifact Rework:
}