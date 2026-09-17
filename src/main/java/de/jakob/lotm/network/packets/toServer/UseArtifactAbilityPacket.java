package main.java.de.jakob.lotm.network.packets.toServer;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactItem;
import de.jakob.lotm.data.ModDataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UseArtifactAbilityPacket() implements CustomPacketPayload {
    public static final Type<UseArtifactAbilityPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "use_artifact_ability"));
    public static final StreamCodec<FriendlyByteBuf, UseArtifactAbilityPacket> STREAM_CODEC =
            StreamCodec.unit(new UseArtifactAbilityPacket());
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static void handle(UseArtifactAbilityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
            InteractionHand hand = InteractionHand.MAIN_HAND;
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (!stack.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                hand = InteractionHand.OFF_HAND;
                stack = serverPlayer.getItemInHand(hand);
                if (!stack.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                    return;
                }
            }
            SealedArtifactItem.tryUseArtifactAbility((ServerLevel) serverPlayer.level(), serverPlayer, hand, stack);
        });
    }
}