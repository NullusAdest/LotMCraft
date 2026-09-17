package de.jakob.lotm.beyonders.potions;

import de.jakob.lotm.util.BeyonderData;
import de.jakob.lotm.util.data.PathwayInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// Sealed Artifacts Rework Imports:
import de.jakob.lotm.beyonders.artifacts.SealedArtifactHandler;
import de.jakob.lotm.beyonders.artifacts.SealedArtifactData;
import de.jakob.lotm.data.ModDataComponents;
import de.jakob.lotm.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.component.DataComponents;
import java.util.ArrayList;

import java.util.List;
import java.util.Properties;
import java.util.Random;

public class BeyonderCharacteristicItem extends Item {
    
    //Rework
    private static final long EXPIRY_TICKS = 20L * 60 * 10; // 10 min(for testing)
    private static final Random RANDOM = new Random();
    private record MergeTarget(int slot, ItemStack stack) {}
    //Rework
    private final String pathway;
    private final int sequence;
    

    public BeyonderCharacteristicItem(Properties properties, String pathway, int sequence) {
        super(properties);

        this.pathway = pathway;
        this.sequence = sequence;
    }

    public String getPathway() {
        return pathway;
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        return Component.literal(PathwayInfos.getSequenceNameByRegisteredItemName(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().replace("_characteristic", "")) + " ").append(Component.translatable("lotm.beyonder_characteristic")).append(
                Component.literal(" (").append(Component.translatable("lotm.sequence")).append(Component.literal(" " + getSequence() + ")")));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if(level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        var item = stack.getItem();

        if(!(item instanceof BeyonderCharacteristicItem beChar)) return InteractionResultHolder.fail(stack);

        int seq = beChar.getSequence();
        String path = beChar.getPathway();

        if(path.equals(BeyonderData.getPathway(player))){
            if(seq >= BeyonderData.getSequence(player)){
                var stacks = BeyonderData.getCharStacks(player);

                if(stacks[seq] >= 0 && seq >= 1 && BeyonderData.getDigestionProgress(player) == 1.0){
                    BeyonderData.setCharStack(player, (stacks[seq] + 1), seq, true);
                    BeyonderData.setDigestionProgress(player, 0);
                    player.setItemInHand(hand, ItemStack.EMPTY);
                    return InteractionResultHolder.success(ItemStack.EMPTY);
                }
            }
        }

        return InteractionResultHolder.fail(stack);
    }
    //Sealed artifacts Rework
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof ServerPlayer player)) return;// player Inventory only expand later DO NOT FORGOT
        Long createdAt = stack.get(ModDataComponents.CHARACTERISTIC_CREATED_AT);
        if (createdAt == null) {
            stack.set(ModDataComponents.CHARACTERISTIC_CREATED_AT, level.getGameTime());
            return;
        }
        if (level.getGameTime() - createdAt < EXPIRY_TICKS) return;
        MergeTarget target = findRandomMergeTarget(player);
        if (target == null) {
            expireIntoSpoiledArtifact((ServerLevel) level, player, slot);
        }
        else{
            expireIntoMergedArtifact(stack, (ServerLevel) level, player, slot, target);
        }
    }

    private void expireIntoSpoiledArtifact(ServerLevel level, ServerPlayer player, int slot) {
        SealedArtifactData data = SealedArtifactHandler.createSealedArtifactData(pathway, sequence, "item");
        ItemStack spoiled = new ItemStack(ModItems.SEALED_ARTIFACT);
        spoiled.set(ModDataComponents.SEALED_ARTIFACT_DATA, data);
        spoiled.set(ModDataComponents.SEALED_ARTIFACT_SELECTED, 0);
        spoiled.set(ModDataComponents.SEALED_ARTIFACT_BASE_TYPE, "item");
        spoiled.set(ModDataComponents.SEALED_ARTIFACT_GENERATED, true);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, player.getSoundSource(), 1.0f, 0.8f);
        player.getInventory().setItem(slot, spoiled);
    }
    private void expireIntoMergedArtifact(ItemStack stack, ServerLevel level, ServerPlayer player, int charSlot, MergeTarget target) {
        ItemStack targetStack = target.stack();
        String baseType = SealedArtifactHandler.getBaseTypeName(targetStack.getItem());
        SealedArtifactData data = SealedArtifactHandler.createSealedArtifactData(pathway, sequence, baseType);
        Component charName = this.getName(stack);
        Component itemName = targetStack.getHoverName();
        ItemStack merged = new ItemStack(targetStack.getItem(), 1);
        merged.set(ModDataComponents.SEALED_ARTIFACT_DATA, data);
        merged.set(ModDataComponents.SEALED_ARTIFACT_SELECTED, 0);
        merged.set(DataComponents.CUSTOM_NAME, Component.translatable("lotm.sealed_artifact.merged", itemName, charName)); // Make it something like the honor name reqirements later like door pathway chat "stick of the stars" or "sword of space"
        targetStack.shrink(1);
        player.getInventory().setItem(target.slot(), targetStack.isEmpty() ? ItemStack.EMPTY : targetStack);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, player.getSoundSource(), 1.0f, 0.8f);
        player.getInventory().setItem(charSlot, merged);
    }
    private MergeTarget findRandomMergeTarget(ServerPlayer player) {
        var inventory = player.getInventory();
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack candidate = inventory.items.get(i);
            if (candidate.isEmpty()) continue;
            if (candidate.getItem() instanceof BeyonderCharacteristicItem) continue;
            if (candidate.has(ModDataComponents.SEALED_ARTIFACT_DATA)) continue; // don't re-merge existing artifacts
            validSlots.add(i);
        }
        if (validSlots.isEmpty()) return null;
        int slot = validSlots.get(RANDOM.nextInt(validSlots.size()));
        return new MergeTarget(slot, inventory.items.get(slot));
    }
}
