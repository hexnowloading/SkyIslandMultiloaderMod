package dev.hexnowloading.dungeonnowloading.entity;

import dev.hexnowloading.dungeonnowloading.item.LifeStealerItem;
import dev.hexnowloading.dungeonnowloading.item.SpawnerSword;
import dev.hexnowloading.dungeonnowloading.registry.DNLItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class DNLEntityEvents {
    public static float onLivingDamageEvent(LivingEntity attacker, LivingEntity target, float damage) {
        ItemStack mainHandItem = attacker.getMainHandItem();
        if (mainHandItem.is(DNLItems.LIFE_STEALER.get())) {
            LifeStealerItem.healthDrain(attacker, damage);
        }
        return damage;
    }

    public static float onLivingHurtEvent(LivingEntity attacker, LivingEntity target, float damage) {
        ItemStack mainHandItem = attacker.getMainHandItem();
        if (mainHandItem.is(DNLItems.SPAWNER_SWORD.get())) {
            damage = SpawnerSword.soulDispersionEffect(attacker, target, damage);
        }
        return damage;
    }
}
