package net.spell_engine.fabric.compat.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.spell_engine.compat.container.ContainerCompat;
import net.spell_engine.internals.container.SpellContainerSource;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import dev.emi.trinkets.api.event.TrinketDropCallback;

import java.util.ArrayList;
import java.util.List;

public class TrinketsCompat {
    private static final String MOD_ID = TrinketsCompatHeader.MOD_ID;
    private static boolean intialized = false;
    private static boolean enabled = false;

    public static boolean init() {
        if (intialized) {
            return enabled;
        }
        intialized = true;
        enabled = FabricLoader.getInstance().isModLoaded(MOD_ID);
        if (!enabled) {
            return enabled;
        }

        ContainerCompat.addProvider(TrinketsCompat::getAll);

        final var spellSourceName = "trinkets";
        SpellContainerSource.addItemSource(
                SpellContainerSource.ItemEntry.of(
                        spellSourceName,
                        (player, name) -> getEquippedStacks(player)
                ),
                SpellContainerSource.MAIN_HAND.name()
        );
        TrinketEquipCallback.EVENT.register((stack, slot, entity) -> {
            if (entity instanceof PlayerEntity player) {
                SpellContainerSource.setDirty(player, spellSourceName);
            }
        });

        TrinketUnequipCallback.EVENT.register((stack, slot, entity) -> {
            if (entity instanceof PlayerEntity player) {
                SpellContainerSource.setDirty(player, spellSourceName);
            }
        });

        TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
            if (entity instanceof PlayerEntity player) {
                SpellContainerSource.setDirty(player, spellSourceName);
            }
            return rule;
        });

        TrinketsItemHelper.register();

        return enabled;
    }

    private static List<ItemStack> getAll(PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return List.of();
        }
        var trinketComponent = component.get();
        return trinketComponent.getAllEquipped().stream().map(reference -> reference.getRight()).toList();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static List<ItemStack> getEquippedStacks(PlayerEntity player) {
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return List.of();
        }
        var equipped = new ArrayList<ItemStack>();
        var trinketComponent = component.get();
        trinketComponent.getAllEquipped().forEach(pair -> {
            var stack = pair.getRight();
            if (stack.isEmpty()) {
                return;
            }
            if (pair.getLeft().getId().contains("spell/book")) {
                equipped.addFirst(stack);
            } else {
                equipped.add(stack);
            }
        });
        return equipped;
    }

    public static ItemStack getSpellBookStack(PlayerEntity player) {
        if (!enabled) {
            return ItemStack.EMPTY;
        }
        var component = TrinketsApi.getTrinketComponent(player);
        if (component.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var spellGroup = component.get().getInventory().get("spell");
        if (spellGroup == null) return ItemStack.EMPTY;

        var trinketInventory = spellGroup.get("book");
        if (trinketInventory == null) return ItemStack.EMPTY;

        Inventory inv = (Inventory) trinketInventory;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}