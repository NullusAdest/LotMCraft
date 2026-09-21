package de.jakob.lotm.network.packets.toServer;

import java.util.List;

import de.jakob.lotm.LOTMCraft;
import de.jakob.lotm.data.ModDataComponents;
import de.jakob.lotm.gui.custom.artifact_wheel.ArtifactWheelMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

//Sealed artifact Rework:
import net.minecraft.world.entity.EquipmentSlot;
//Sealed artifact Rework:

public record SyncArtifactAbilityWheel (int index) implements CustomPacketPayload {

    public static final Type<SyncArtifactAbilityWheel> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(LOTMCraft.MOD_ID, "sync_artifact_ability_wheel"));

    public static final StreamCodec<ByteBuf, SyncArtifactAbilityWheel> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncArtifactAbilityWheel::index,
            SyncArtifactAbilityWheel::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    //Sealed artifact Rework
    public static void handle(SyncArtifactAbilityWheel packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            List<ArtifactWheelMenu.WheelEntry> entries = ArtifactWheelMenu.buildEntries(player);

            if (packet.index() < 0 || packet.index() >= entries.size()) return;

            ArtifactWheelMenu.WheelEntry entry = entries.get(packet.index());

            for (EquipmentSlot slot : ArtifactWheelMenu.SEALED_ARTIFACT_SLOTS) {
                ItemStack equipped = player.getItemBySlot(slot);
                if (equipped.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                    equipped.set(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, false);
                }
            }

            ItemStack target = player.getItemBySlot(entry.slot());
            target.set(ModDataComponents.SEALED_ARTIFACT_SELECTED, entry.localIndex());
            target.set(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, true);
        });
    }
    //Sealed Artifact Rework
}
