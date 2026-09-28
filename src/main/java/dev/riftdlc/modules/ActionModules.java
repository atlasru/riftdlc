package dev.riftdlc.modules;

import dev.riftdlc.RiftDLC;
import dev.riftdlc.core.RiftModule;
import dev.riftdlc.core.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Small independent tick modules. Each case has a concrete client action. */
public final class ActionModules extends RiftModule {
    public enum Action {
        AUTO_JUMP, SPEED, SAFE_WALK, AUTO_SWIM, FAST_CLIMB,
        TRIGGER_BOT, KILL_AURA, AUTO_EAT, AUTO_TOOL, AUTO_RESPAWN,
        FULLBRIGHT, AUTO_MINE, AUTO_PLACE, NO_FALL, AUTO_SNEAK
    }
    private final Action action;
    private final Setting<Integer> range;
    private final Setting<Integer> strength;
    private final Setting<Boolean> players;
    private final Setting<Boolean> mobs;
    private int oldSlot = -1;
    private double oldGamma;
    private boolean changedGamma;
    public ActionModules(Action action) {
        super(action.name().toLowerCase(), display(action), description(action), category(action));
        this.action = action;
        range = switch (action) {
            case KILL_AURA -> add(Setting.integer("range", "Maximum target distance", 4, 1, 6));
            case AUTO_EAT -> add(Setting.integer("hunger", "Start eating below this hunger", 14, 1, 19));
            default -> null;
        };
        strength = action == Action.SPEED ? add(Setting.integer("percent", "Horizontal speed increase", 10, 1, 25)) : null;
        players = action == Action.KILL_AURA ? add(Setting.bool("players", "Target non-friend players", true)) : null;
        mobs = action == Action.KILL_AURA ? add(Setting.bool("mobs", "Target other living entities", false)) : null;
    }
    private static String display(Action a) {
        return switch (a) {
            case TRIGGER_BOT -> "Trigger Bot";
            case KILL_AURA -> "Kill Aura";
            case SAFE_WALK -> "Safe Walk";
            case FAST_CLIMB -> "Fast Climb";
            case NO_FALL -> "No Fall (client)";
            default -> java.util.Arrays.stream(a.name().split("_")).map(s -> s.charAt(0) + s.substring(1).toLowerCase()).reduce((x, y) -> x + " " + y).orElse(a.name());
        };
    }
    private static String description(Action a) {
        return switch (a) {
            case AUTO_JUMP -> "Jump while moving on the ground";
            case SPEED -> "Modest horizontal momentum boost while sprinting; servers may correct movement";
            case SAFE_WALK -> "Hold sneak at exposed block edges";
            case AUTO_SWIM -> "Swim upward while moving in water";
            case FAST_CLIMB -> "Climb ladders faster when pressing forward";
            case TRIGGER_BOT -> "Attack living entity under crosshair when attack cooldown is ready";
            case KILL_AURA -> "Attack nearest visible non-friend within range at normal cooldown";
            case AUTO_EAT -> "Select food from hotbar and hold use when hungry";
            case AUTO_TOOL -> "Select fastest hotbar tool for targeted block while mining";
            case AUTO_RESPAWN -> "Respawn after death screen appears";
            case FULLBRIGHT -> "Increase local gamma and restore it on disable";
            case AUTO_MINE -> "Hold attack on a targeted block";
            case AUTO_PLACE -> "Hold use while aiming at a block with an item";
            case NO_FALL -> "Reset client fall counter; only affects local prediction";
            case AUTO_SNEAK -> "Hold sneak while walking";
        };
    }
    private static Category category(Action a) {
        return switch (a) {
            case AUTO_JUMP, SPEED, SAFE_WALK, AUTO_SWIM, FAST_CLIMB, AUTO_SNEAK, NO_FALL -> Category.MOVEMENT;
            case TRIGGER_BOT, KILL_AURA -> Category.COMBAT;
            case AUTO_EAT, AUTO_TOOL, AUTO_RESPAWN -> Category.PLAYER;
            case FULLBRIGHT -> Category.RENDER;
            case AUTO_MINE, AUTO_PLACE -> Category.WORLD;
        };
    }
    @Override public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        var p = mc.player;
        if (action == Action.AUTO_RESPAWN) {
            if (mc.gui.screen() instanceof DeathScreen) p.respawn();
            return;
        }
        if (mc.gui.screen() != null) { releaseKeys(mc); return; }
        switch (action) {
            case AUTO_JUMP -> {
                if (p.onGround() && p.input.keyPresses.forward() && !p.isCrouching()) p.jumpFromGround();
            }
            case SPEED -> {
                if (p.onGround() && p.isSprinting() && p.input.keyPresses.forward()) {
                    Vec3 v = p.getDeltaMovement();
                    if (v.horizontalDistanceSqr() > 0.0001 && v.horizontalDistanceSqr() < 0.6)
                        p.setDeltaMovement(v.x * (1 + strength.get() / 100.0), v.y, v.z * (1 + strength.get() / 100.0));
                }
            }
            case SAFE_WALK -> {
                var v = p.getDeltaMovement();
                var ahead = net.minecraft.core.BlockPos.containing(p.getX() + v.x * 2, p.getY() - 0.5, p.getZ() + v.z * 2);
                mc.options.keyShift.setDown(p.onGround() && mc.level.getBlockState(ahead).isAir());
            }
            case AUTO_SWIM -> {
                if (p.isInWater() && p.input.keyPresses.forward()) p.setDeltaMovement(p.getDeltaMovement().add(0, 0.04, 0));
            }
            case FAST_CLIMB -> {
                if (p.onClimbable() && p.input.keyPresses.forward()) {
                    Vec3 v = p.getDeltaMovement(); p.setDeltaMovement(v.x, Math.max(v.y, 0.26), v.z);
                }
            }
            case TRIGGER_BOT -> {
                if (mc.hitResult instanceof EntityHitResult hit && valid(hit.getEntity(), mc, 4) && p.getAttackStrengthScale(0) >= 0.98f)
                    attack(hit.getEntity(), mc);
            }
            case KILL_AURA -> {
                if (p.getAttackStrengthScale(0) < 0.98f) break;
                var targets = mc.level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range.get()),
                    e -> valid(e, mc, range.get()) && (e instanceof Player ? players.get() : mobs.get()));
                targets.stream().min(java.util.Comparator.comparingDouble(p::distanceToSqr)).ifPresent(e -> attack(e, mc));
            }
            case AUTO_EAT -> {
                if (p.getFoodData().getFoodLevel() >= range.get() || p.isUsingItem() && oldSlot < 0) { finishEating(mc); break; }
                if (oldSlot < 0) {
                    for (int i = 0; i < 9; i++) if (p.getInventory().getItem(i).getComponents().has(DataComponents.FOOD)) {
                        oldSlot = p.getInventory().getSelectedSlot(); p.getInventory().setSelectedSlot(i); break;
                    }
                }
                if (oldSlot >= 0) mc.options.keyUse.setDown(true);
            }
            case AUTO_TOOL -> {
                if (!mc.options.keyAttack.isDown() || !(mc.hitResult instanceof BlockHitResult hit)) break;
                var state = mc.level.getBlockState(hit.getBlockPos());
                float best = p.getInventory().getSelectedItem().getDestroySpeed(state);
                int slot = -1;
                for (int i = 0; i < 9; i++) {
                    float speed = p.getInventory().getItem(i).getDestroySpeed(state);
                    if (speed > best) { best = speed; slot = i; }
                }
                if (slot >= 0) p.getInventory().setSelectedSlot(slot);
            }
            case FULLBRIGHT -> {
                if (!changedGamma) { oldGamma = mc.options.gamma().get(); changedGamma = true; }
                mc.options.gamma().set(1.0);
            }
            case AUTO_MINE -> mc.options.keyAttack.setDown(mc.hitResult instanceof BlockHitResult);
            case AUTO_PLACE -> mc.options.keyUse.setDown(mc.hitResult instanceof BlockHitResult && !p.getMainHandItem().isEmpty());
            case NO_FALL -> { if (p.fallDistance > 0) p.resetFallDistance(); }
            case AUTO_SNEAK -> mc.options.keyShift.setDown(p.input.keyPresses.forward());
            default -> {}
        }
    }
    private boolean valid(Entity entity, Minecraft mc, int distance) {
        return entity instanceof LivingEntity living && entity != mc.player && living.isAlive() && !living.isSpectator()
            && mc.player.distanceToSqr(entity) <= distance * distance
            && (entity instanceof Player ? !RiftDLC.instance().friends().contains(entity.getName().getString()) : true)
            && mc.player.hasLineOfSight(entity);
    }
    private void attack(Entity entity, Minecraft mc) {
        if (mc.gameMode != null) { mc.gameMode.attack(mc.player, entity); mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true); }
    }
    private void finishEating(Minecraft mc) {
        if (oldSlot >= 0) {
            mc.options.keyUse.setDown(false);
            mc.player.getInventory().setSelectedSlot(oldSlot); oldSlot = -1;
        }
    }
    private void releaseKeys(Minecraft mc) {
        if (action == Action.AUTO_EAT) finishEating(mc);
        if (action == Action.AUTO_MINE) mc.options.keyAttack.setDown(false);
        if (action == Action.AUTO_PLACE) mc.options.keyUse.setDown(false);
        if (action == Action.SAFE_WALK || action == Action.AUTO_SNEAK) mc.options.keyShift.setDown(false);
    }
    @Override protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) releaseKeys(mc);
        if (action == Action.FULLBRIGHT && changedGamma) { mc.options.gamma().set(oldGamma); changedGamma = false; }
    }
}
