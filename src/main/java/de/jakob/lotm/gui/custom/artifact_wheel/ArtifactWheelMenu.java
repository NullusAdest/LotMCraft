package de.jakob.lotm.gui.custom.artifact_wheel;

import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.data.ModDataComponents;
import de.jakob.lotm.gui.ModMenuTypes;
import de.jakob.lotm.network.PacketHandler;
import de.jakob.lotm.network.packets.toServer.SyncArtifactAbilityWheel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
//Sealed artifact Rework:
import net.minecraft.world.entity.EquipmentSlot;
import de.jakob.lotm.beyonders.abilities.core.Ability;
//Sealed artifact Rework:

//Sealed artifact Rework
public class ArtifactWheelMenu extends AbstractContainerMenu {

    public static final EquipmentSlot[] SEALED_ARTIFACT_SLOTS = {
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    public record WheelEntry(EquipmentSlot slot, int localIndex, String abilityId) {}

    private final Inventory playerInventory;
    private final List<WheelEntry> entries;

    public ArtifactWheelMenu(int containerId, Inventory playerInventory, ItemStack stack) {
        super(ModMenuTypes.ARTIFACT_WHEEL_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.entries = buildEntries(playerInventory.player);
    }

    public static List<WheelEntry> buildEntries(Player player) {
        List<WheelEntry> result = new ArrayList<>();
        for (EquipmentSlot slot : SEALED_ARTIFACT_SLOTS) {
            ItemStack equipped = player.getItemBySlot(slot);
            SealedArtifactData data = equipped.get(ModDataComponents.SEALED_ARTIFACT_DATA);
            if (data == null || data.abilities().isEmpty()) continue;
            List<Ability> abilities = data.abilities();
            for (int i = 0; i < abilities.size(); i++) {result.add(new WheelEntry(slot, i, abilities.get(i).getId()));}
        }
        return result;
    }

    public List<String> getAbilities() {
        return entries.stream().map(WheelEntry::abilityId).toList();
    }

    public int getSelectedAbilityIndex() {
        Player player = playerInventory.player;
        for (int i = 0; i < entries.size(); i++) {
            WheelEntry entry = entries.get(i);
            ItemStack equipped = player.getItemBySlot(entry.slot());
            boolean active = equipped.getOrDefault(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, false);
            int localSelected = equipped.getOrDefault(ModDataComponents.SEALED_ARTIFACT_SELECTED, 0);
            if (active && localSelected == entry.localIndex()) {return i;}
        }
        return 0;
    }

    public void setSelectedAbilityIndex(int flatIndex) {
        if (flatIndex < 0 || flatIndex >= entries.size()) return;
        WheelEntry entry = entries.get(flatIndex);
        Player player = playerInventory.player;
        for (EquipmentSlot slot : SEALED_ARTIFACT_SLOTS) {
            ItemStack equipped = player.getItemBySlot(slot);
            if (equipped.has(ModDataComponents.SEALED_ARTIFACT_DATA)) {
                equipped.set(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, false);
            }
        }
        ItemStack target = player.getItemBySlot(entry.slot());
        target.set(ModDataComponents.SEALED_ARTIFACT_SELECTED, entry.localIndex());
        target.set(ModDataComponents.SEALED_ARTIFACT_WHEEL_ACTIVE, true);
        PacketHandler.sendToServer(new SyncArtifactAbilityWheel(flatIndex));
    }
    // Sealed Artifcat Rework:
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}