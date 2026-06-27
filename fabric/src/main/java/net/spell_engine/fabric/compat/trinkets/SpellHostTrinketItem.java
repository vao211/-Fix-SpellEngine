package net.spell_engine.fabric.compat.trinkets;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Map;

public class SpellHostTrinketItem extends TrinketItem {
    private final SoundEvent equipSound;

    public SpellHostTrinketItem(Settings settings, SoundEvent equipSound) {
        super(settings);
        this.equipSound = equipSound;
    }

    @Override
    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiers(ItemStack stack, SlotReference slot, LivingEntity entity, Identifier slotIdentifier) {
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> uniqueModifiers = ArrayListMultimap.create();

        try {
            Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> defaultModifiers = super.getModifiers(stack, slot, entity, slotIdentifier);
            String itemName = Registries.ITEM.getId(stack.getItem()).getPath();

            for (Map.Entry<RegistryEntry<EntityAttribute>, EntityAttributeModifier> entry : defaultModifiers.entries()) {
                EntityAttributeModifier modifier = entry.getValue();
                String attributeName = entry.getKey().value().getTranslationKey().replace("attribute.name.", "");

                String rawPath = slotIdentifier.getPath() + "_" + itemName + "_base_" + attributeName + "_" + modifier.operation().name();

                String safePath = rawPath.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");

                uniqueModifiers.put(entry.getKey(), new EntityAttributeModifier(
                        Identifier.of(slotIdentifier.getNamespace(), safePath),
                        modifier.value(),
                        modifier.operation()
                ));
            }
        } catch (Exception ignored) {
        }

        return uniqueModifiers;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        var isOnCooldown = false;
        if (entity instanceof PlayerEntity player) {
            isOnCooldown = !player.isCreative() && player.getItemCooldownManager().isCoolingDown(stack.getItem());
        }
        return super.canUnequip(stack, slot, entity) && !isOnCooldown;
    }

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        super.onEquip(stack, slot, entity);

        if (entity.getWorld().isClient() // Play sound only on client
                && entity.age > 100      // Avoid playing sound on entering world / dimension
        ) {
            entity.playSound(this.equipSound, 1.0F, 1.0F);
        }
    }
}