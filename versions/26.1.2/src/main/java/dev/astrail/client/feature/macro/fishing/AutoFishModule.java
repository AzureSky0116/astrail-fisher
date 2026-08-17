package dev.astrail.client.feature.macro.fishing;

import dev.astrail.client.AstrailClient;
import dev.astrail.client.api.event.ClientTickEvent;
import dev.astrail.client.api.module.BackgroundRunning;
import dev.astrail.client.api.module.DisableReason;
import dev.astrail.client.api.module.ModuleCategory;
import dev.astrail.client.api.module.ModuleMetadata;
import dev.astrail.client.api.service.InputAction;
import dev.astrail.client.api.service.InputLease;
import dev.astrail.client.api.service.InteractionService;
import dev.astrail.client.api.service.RotationLease;
import dev.astrail.client.api.setting.BooleanSetting;
import dev.astrail.client.api.setting.NumberSetting;
import dev.astrail.client.core.ClientServices;
import dev.astrail.client.core.event.EventBus;
import dev.astrail.client.core.module.AbstractModule;
import dev.astrail.client.core.module.ModuleScope;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class AutoFishModule extends AbstractModule implements BackgroundRunning {
    public static final String ID = "macro.auto_fish";

    /**
     * Ticks with a rod in hand, no hook in the water and no cast queued before
     * one is thrown.
     *
     * <p>Casting used to be a side effect of Auto Reset's "hook vanished"
     * recovery, whose window is three seconds — so enabling the module sat idle
     * for that long before the first cast, and with Auto Reset off it never cast
     * at all. This window is short enough to feel immediate but still leaves
     * room for the hook entity to arrive from the server before concluding that
     * nothing is in the water.
     */
    private static final int IDLE_CAST_TICKS = 14;

    /**
     * Minimum ticks between two idle casts. With no hook echo on a high-ping
     * server the old 14-tick window could fire a second use-item before the
     * first bobber arrived, reeling the fresh cast back in.
     */
    private static final int IDLE_CAST_RETRY_TICKS = 40;

    /**
     * Large enough to clear the player footprint, small enough that narrow
     * fishing ledges can still accept a sideways shuffle.
     */
    private static final double STEP_PROBE_BASE = 0.9D;

    /**
     * Ticks of coasting the probe budgets for on top of {@link #STEP_PROBE_BASE}.
     *
     * <p>Releasing a movement key does not stop the player: friction bleeds the
     * velocity off over several ticks, and Speed (or Depth Strider, ice, a
     * dolphin's grace) raises how far that carries. A fixed-distance probe was
     * therefore safe at walking pace and useless under a speed effect 鈥?the
     * player cleared the checked block inside one tick and left the ledge before
     * the next check ran. The probe now looks as far as the current velocity
     * would actually take them.
     */
    private static final int STEP_COAST_TICKS = 12;

    /** Spacing of the samples along the probe, in blocks. */
    private static final double STEP_PROBE_SPACING = 0.25D;

    /** Player half-width plus a small edge margin for ground sampling. */
    private static final double PLAYER_FOOTPRINT_RADIUS = 0.36D;

    /** How far the pre-cast aim check follows the look line, in blocks. */
    private static final double CAST_AIM_RANGE = 6.0D;
    private static final double CAST_TARGET_RANGE_SQUARED = 14.0D * 14.0D;
    private static final int CAST_WATER_SEARCH_RADIUS = 6;
    private static final int CAST_FALLBACK_SEARCH_RADIUS = 12;

    /**
     * Ticks a cast may be held back waiting for water before going out anyway,
     * so a drained pond or a shoved player cannot stall the module.
     */
    private static final int CAST_HOLD_LIMIT = 60;

    /**
     * How far the footwork may stray from where the catch happened, in blocks.
     * Wandering off also aims the next cast at dry land.
     */
    private static final double STEP_LEASH = 2.6D;

    /**
     * Ticks a freshly cast hook must survive before a bite marker counts.
     *
     * <p>The marker entity from the previous catch outlives the reel by a few
     * ticks and lands within range of the next hook, which sits at almost the
     * same spot. Without this window the stale marker read as an immediate bite
     * and the rod was reeled a fraction of a second after casting, over and over.
     */
    private static final int BITE_ARM_TICKS = 6;

    /**
     * Horizontal-and-vertical radius, squared, around the hook that a bite
     * marker must sit inside. Six blocks also caught the markers of players
     * fishing beside you at a shared pond, which reeled your rod on their
     * catches; the marker itself spawns essentially on top of the bobber.
     */
    private static final double BITE_MARKER_RADIUS_SQUARED = 9.0D;
    /**
     * Ticks after a bite during which a newly spawned sea creature counts as
     * this catch's monster. Long enough for the server to spawn the creature
     * after the reel; ownership is still kept exact by the spawn-radius check.
     */
    private static final int MONSTER_DETECTION_TICKS = 60;
    /**
     * A freshly caught sea creature spawns where the bobber was reeled in, so
     * only a new living entity this close to the catch spot can be this
     * catch's monster. Kept tight on purpose: a generous radius (or the old
     * near-player check) picked up neighbouring players' creatures at shared
     * ponds.
     */
    private static final double MONSTER_SPAWN_RANGE_SQUARED = 8.0D * 8.0D;
    /**
     * Loose keep-fighting envelope used once a creature is owned (detection
     * uses the tighter spawn radius above).
     */
    private static final double MONSTER_HOOK_RANGE_SQUARED = 14.0D * 14.0D;
    private static final double MONSTER_PLAYER_RANGE_SQUARED = 10.0D * 10.0D;
    /**
     * Hard cap on one attack: if the creature survives this long (or the
     * server never tells us it died, e.g. someone else killed it), stop
     * clicking and go back to fishing instead of draining mana forever.
     */
    private static final int ATTACK_TIMEOUT_TICKS = 20 * 20;
    private static final int ATTACK_AIM_TIMEOUT_TICKS = 30;
    private static final int ATTACK_VIEW_RESTORE_TIMEOUT_TICKS = 40;
    private static final double ATTACK_AIM_COSINE = Math.cos(Math.toRadians(3.0D));

    /**
     * Lowercase names of every sea creature in the official SkyBlock wiki water
     * and lava lists, plus community-reported humanoid creatures. Humanoid
     * creatures (Banshee, Frog Man, the Spooky set, the sharks, Alligator,
     * ...) render as fake player entities on Hypixel, and some of those NPC
     * players do appear in the tab list — the name is then the signal that
     * overrides the tab-list check. Names shorter than five letters (e.g.
     * "Yeti", "Ent") are never matched freely, so ordinary player names cannot
     * false-positive; those creatures fall back to the tab-list check.
     */
    private static final Set<String> KNOWN_SEA_CREATURE_NAMES = Set.of(
        "squid", "sea walker", "night squid", "sea guardian", "sea witch", "sea archer",
        "rider of the deep", "catfish", "carrot king", "agarimoo", "sea leech",
        "guardian defender", "deep sea protector", "water hydra", "oasis rabbit",
        "oasis sheep", "water worm", "poisoned water worm", "abyssal miner",
        "scarecrow", "nightmare", "werewolf", "phantom fisher", "grim reaper",
        "frozen steve", "frosty", "grinch", "nutcracker", "yeti", "reindrake",
        "nurse shark", "blue shark", "tiger shark", "great white shark",
        "trash gobbler", "dumpster diver", "banshee", "bayou sludge", "alligator",
        "titanoboa", "frog man", "snapping turtle", "blue ringed octopus", "wiki tiki",
        "bogged", "wetwing", "tadgang", "ent", "the loch emperor", "nessie",
        "flaming worm", "lava blaze", "lava pigman", "magma slug", "moogma",
        "lava leech", "pyroclastic worm", "lava flame", "fire eel", "taurus",
        "plhlegblast", "thunder", "lord jawbus", "fried chicken", "fireproof witch",
        "fiery scuttler", "ragnarok", "stridersurfer",
        "jumpin' jack", "jumping jack"
    );

    private final BooleanSetting move = new BooleanSetting("move", "Random Movement", true);
    private final BooleanSetting alwaysSneak = new BooleanSetting("always_sneak", "Always Sneak", true);
    private final BooleanSetting autoReset = new BooleanSetting("auto_reset", "Auto Reset", true);
    private final NumberSetting resetTimeout = new NumberSetting("reset_timeout_seconds", "Reset Timeout", 20.0D, 5.0D, 120.0D, 1.0D);
    private final NumberSetting stuckLimit = new NumberSetting("stuck_limit", "Retry Limit", 5.0D, 1.0D, 50.0D, 1.0D);
    private final NumberSetting throwDelay = new NumberSetting("throw_delay_ticks", "Throw Delay", 10.0D, 1.0D, 30.0D, 1.0D);
    private final BooleanSetting viewLock = new BooleanSetting("view_lock", "View Lock", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate", "Subtle Rotation", true);
    private final BooleanSetting autoAttack = new BooleanSetting("auto_attack", "Auto Attack", false);
    private final NumberSetting attackCps = new NumberSetting("attack_cps", "Attack CPS", 5.0D, 1.0D, 10.0D, 0.5D);
    private final BooleanSetting aimBeforeAttack = new BooleanSetting("aim_before_attack", "Aim Before Attack", true);
    private final BooleanSetting singleUse = new BooleanSetting("single_use", "Single Use", false);
    private final NumberSetting weaponSlot = new NumberSetting("weapon_slot", "Weapon Slot", 1.0D, 1.0D, 9.0D, 1.0D);

    private final EventBus events;
    private final dev.astrail.client.api.service.InputService inputs;
    private final dev.astrail.client.api.service.RotationService rotations;
    private final InteractionService interactions;
    private final Random random = new Random();

    private InputLease sneakLease;
    private InputLease forwardLease;
    private InputLease backwardLease;
    private InputLease leftLease;
    private InputLease rightLease;
    private RotationLease rotationLease;
    private float originalYaw;
    private float originalPitch;
    private long tick;
    private int hookAge;
    private int noHookAge;
    private int recastCountdown = -1;
    private int motionTick = -1;
    private int motionLength;

    /**
     * Movement plan for the current catch: a short run of legs, each holding a
     * primary direction and optionally a second one alongside it for a diagonal.
     * A null primary is a deliberate pause.
     *
     * <p>Two fixed legs read as a metronome no matter how their timings were
     * drawn 鈥?out, back, done, every single catch. A variable-length run of
     * uneven legs, some of them pauses and some diagonal, does not.
     */
    private final java.util.List<Leg> legs = new java.util.ArrayList<>();

    private record Leg(InputAction primary, InputAction secondary, int start, int end) {}

    /** Where the catch happened; the footwork stays leashed to it. */
    private Vec3 motionAnchor = Vec3.ZERO;
    private float castYaw;
    private float castPitch;
    private Vec3 castWaterTarget = Vec3.ZERO;
    /** Ticks the queued cast has been held back waiting for water to aim at. */
    private int castHeldTicks;
    private int castTargetSearchCooldown;
    private boolean biteLatched;
    private long lastBiteTick = Long.MIN_VALUE / 2L;
    /** Entity id of the marker already reeled on, so one marker reels once. */
    private int reeledMarkerId = -1;
    private float yawOffset;
    private float pitchOffset;
    private final Set<Integer> preCatchLivingEntities = new HashSet<>();
    private Vec3 catchPosition = Vec3.ZERO;
    private int monsterDetectionTicks;
    private int skillUseCooldown;
    private int previousHotbarSlot = -1;
    private int attackHotbarSlot = -1;
    private int attackTargetId = -1;
    private Vec3 attackAimPoint = Vec3.ZERO;
    private float attackOriginalYaw;
    private float attackOriginalPitch;
    private int attackAimTicks;
    private int attackTicks;
    private int attackViewRestoreTicks;
    private boolean restoringAttackView;
    private boolean pendingCatchMovement;
    private int movementSafetyTicks;
    private InputLease jumpLease;
    /** Consecutive stuck-hook resets with no catch in between. */
    private int stuckStreak;
    private long lastIdleCastTick = Long.MIN_VALUE / 2L;

    public AutoFishModule(ClientServices services) {
        super(new ModuleMetadata(ID, "Auto Fishing", "Reels, recasts and gently varies fishing input", ModuleCategory.MACRO, true, true));
        events = services.events();
        inputs = services.inputs();
        rotations = services.rotations();
        interactions = services.interactions();
        move.presentation("Humanization", "Randomly shift your footing after a catch.");
        alwaysSneak.presentation("Safety", "Sneak for the full fishing session.");
        autoReset.presentation("Recovery", "Reset when the hook stalls or vanishes.");
        resetTimeout.presentation("Recovery", "Seconds without a bite before the hook is recast.");
        stuckLimit.presentation("Recovery", "Disable auto fishing after this many stuck retries.");
        throwDelay.presentation("Timing", "Ticks before recasting.");
        viewLock.presentation("Casting", "Re-aim at the water before recasting. Off: your view stays free.");
        rotate.presentation("Humanization", "Add subtle camera drift after a catch.");
        autoAttack.presentation("Combat", "Use a weapon ability when a sea creature appears.");
        attackCps.presentation("Combat", "Right-clicks per second while fighting.");
        aimBeforeAttack.presentation("Combat", "Aim before attacking; off for teleport weapons (e.g. Hyperion).");
        singleUse.presentation("Combat", "Ability once per sea creature, then stop. Saves mana.");
        weaponSlot.presentation("Combat", "Hotbar slot containing the ability weapon (1-9).");
        addSettings(move, alwaysSneak, autoReset, resetTimeout, stuckLimit, throwDelay, viewLock, rotate,
            autoAttack, attackCps, aimBeforeAttack, singleUse, weaponSlot);
    }

    @Override
    protected void onEnable(ModuleScope scope) {
        sneakLease = scope.own(inputs.acquire(ID, InputAction.SNEAK));
        forwardLease = scope.own(inputs.acquire(ID, InputAction.FORWARD));
        backwardLease = scope.own(inputs.acquire(ID, InputAction.BACKWARD));
        leftLease = scope.own(inputs.acquire(ID, InputAction.LEFT));
        rightLease = scope.own(inputs.acquire(ID, InputAction.RIGHT));
        jumpLease = scope.own(inputs.acquire(ID, InputAction.JUMP));
        rotationLease = scope.own(rotations.acquire(ID, 45));
        resetState();
        sneakLease.setPressed(alwaysSneak.get());
        scope.own(events.subscribe(ClientTickEvent.class, ID, this::onTick));
    }

    @Override
    protected void onDisable(DisableReason reason) {
        cancelAutoAttack(Minecraft.getInstance().player);
        releaseMovement();
    }

    private void onTick(ClientTickEvent event) {
        tick++;
        LocalPlayer player = event.client().player;
        if (player == null || event.client().level == null) {
            cancelAutoAttack(player);
            releaseMovement();
            return;
        }

        // Combat temporarily replaces the fishing rod in the main hand. It
        // must run before the rod guard, otherwise the first weapon tick would
        // abort the whole fishing state machine.
        if (updateAutoAttack(event.client(), player)) {
            releaseDirectionalMovement();
            sneakLease.setPressed(alwaysSneak.get() || movementSafetyTicks > 0);
            return;
        }
        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            releaseMovement();
            return;
        }

        FishingHook hook = ownedHook(event.client(), player);
        boolean hasHook = hook != null;
        if (hasHook) {
            hookAge++;
            noHookAge = 0;
        } else {
            noHookAge++;
            hookAge = 0;
        }

        Entity marker = hasHook && hookAge >= BITE_ARM_TICKS ? biteMarkerNear(event.client(), hook) : null;
        boolean bite = marker != null && marker.getId() != reeledMarkerId;
        if (bite && !biteLatched && tick - lastBiteTick >= 10L && recastCountdown < 0) {
            reeledMarkerId = marker.getId();
            boolean detectMonster = autoAttack.get();
            if (detectMonster) beginMonsterDetection(event.client(), player, hook);
            reelAndQueueCast(player, throwDelay.intValue(), !detectMonster, hook.position());
            pendingCatchMovement = detectMonster;
            stuckStreak = 0;
            lastBiteTick = tick;
        }
        biteLatched = bite;

        // Casting an idle rod is the module's core job, not a recovery step, so
        // it no longer waits on Auto Reset being enabled or on that feature's
        // much longer window.
        if (!hasHook && recastCountdown < 0 && noHookAge >= IDLE_CAST_TICKS
            && tick - lastIdleCastTick >= IDLE_CAST_RETRY_TICKS) {
            lastIdleCastTick = tick;
            interactions.useMainHand();
            noHookAge = 0;
        }
        if (autoReset.get() && recastCountdown < 0 && hasHook && hookAge >= resetTimeout.intValue() * 20) {
            reelAndQueueCast(player, 20, false, hook.position());
            stuckStreak++;
            if (stuckStreak >= stuckLimit.intValue()) {
                player.sendSystemMessage(Component.literal(
                    "Astrail Fisher: " + stuckLimit.intValue() + " stuck hooks in a row - auto fishing disabled."
                ).withStyle(ChatFormatting.RED));
                disable(DisableReason.USER);
                AstrailClient.runtime().config().markDirty();
                return;
            }
        }

        updateMotion(player);
        if (recastCountdown >= 0 && --recastCountdown <= 0) {
            if (holdCast(player)) {
                castHeldTicks++;
                recastCountdown = 1;
            } else {
                rotationLease.clear();
                interactions.useMainHand();
                recastCountdown = -1;
                castHeldTicks = 0;
                hookAge = 0;
                noHookAge = 0;
            }
        }
        if (movementSafetyTicks > 0) movementSafetyTicks--;
        // Sneak only while a step is actually being taken (and for a short
        // coast after it), not for the whole humanisation phase: the motion
        // stage also runs when Random Movement is off or no safe direction
        // exists, and holding sneak through it made the player crouch on
        // every single catch.
        sneakLease.setPressed(alwaysSneak.get()
            || movementSafetyTicks > 0);
    }

    private void beginMonsterDetection(Minecraft client, LocalPlayer player, FishingHook hook) {
        preCatchLivingEntities.clear();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) preCatchLivingEntities.add(entity.getId());
        }
        catchPosition = hook.position();
        monsterDetectionTicks = MONSTER_DETECTION_TICKS;
        skillUseCooldown = 0;
        previousHotbarSlot = -1;
        attackHotbarSlot = -1;
        attackTargetId = -1;
        restoringAttackView = false;
    }

    /** Updates the monster detection/ability state and claims the tick while active. */
    private boolean updateAutoAttack(Minecraft client, LocalPlayer player) {
        boolean active = monsterDetectionTicks > 0
            || attackTargetId >= 0
            || previousHotbarSlot >= 0
            || restoringAttackView;
        if (!active) return false;

        if (!autoAttack.get()) {
            boolean resumeMovement = pendingCatchMovement;
            cancelAutoAttack(player);
            if (resumeMovement) beginMotion(player);
            return false;
        }

        if (restoringAttackView) {
            requestLook(player, attackOriginalYaw, attackOriginalPitch, 10.0F, 0.62F);
            attackViewRestoreTicks++;
            if (!isViewNear(player, attackOriginalYaw, attackOriginalPitch, 1.0F)
                && attackViewRestoreTicks < ATTACK_VIEW_RESTORE_TIMEOUT_TICKS) {
                return true;
            }
            player.setYRot(attackOriginalYaw);
            player.setXRot(attackOriginalPitch);
            finishAttackViewRestore(player);
            return true;
        }

        if (attackTargetId >= 0) {
            if (attackHotbarSlot < 0 || attackHotbarSlot > 8) {
                boolean resumeMovement = pendingCatchMovement;
                cancelAutoAttack(player);
                if (resumeMovement) beginMotion(player);
                return true;
            }
            var weapon = player.getInventory().getItem(attackHotbarSlot);
            if (weapon.isEmpty()) {
                boolean resumeMovement = pendingCatchMovement;
                cancelAutoAttack(player);
                if (resumeMovement) beginMotion(player);
                return true;
            }
            Entity target = client.level.getEntity(attackTargetId);
            // isDeadOrDying also covers the death animation: when another
            // player kills our creature the client may keep the entity around
            // for a few ticks before removing it.
            if (!(target instanceof LivingEntity living) || !living.isAlive() || living.isDeadOrDying()) {
                beginAttackViewRestore(player);
                return true;
            }
            // The creature can stay alive far outside weapon range after a
            // knockback or a teleport; clicking blindly then just drains mana
            // into the air, so the attack ends once it leaves the envelope.
            if (!isAttackTargetAround(player, living)) {
                beginAttackViewRestore(player);
                return true;
            }
            // Belt and braces: whatever the server does with the creature
            // (killed by a neighbour, stuck unkillable, never despawned),
            // one attack can never click forever.
            attackTicks++;
            if (attackTicks > ATTACK_TIMEOUT_TICKS) {
                beginAttackViewRestore(player);
                return true;
            }
            player.getInventory().setSelectedSlot(attackHotbarSlot);
            if (aimBeforeAttack.get()) {
                attackAimPoint = living.getEyePosition();
                requestAttackLook(attackAimPoint);
                attackAimTicks++;
                if (!isLookingAt(player, attackAimPoint)
                    && attackAimTicks < ATTACK_AIM_TIMEOUT_TICKS) {
                    return true;
                }
            } else {
                attackAimTicks = 0;
            }
            if (skillUseCooldown > 0 && --skillUseCooldown > 0) return true;
            interactions.useMainHand();
            skillUseCooldown = Math.max(1, (int) Math.round(20.0D / attackCps.get()));
            attackAimTicks = 0;
            if (singleUse.get()) {
                beginAttackViewRestore(player);
            }
            return true;
        }

        Entity monster = findCaughtMonster(client);
        if (monster != null && startAutoAttack(player, monster)) return true;
        monsterDetectionTicks--;
        if (monsterDetectionTicks <= 0) {
            preCatchLivingEntities.clear();
            catchPosition = Vec3.ZERO;
            if (pendingCatchMovement) {
                beginMotion(player);
                pendingCatchMovement = false;
            }
            return false;
        }
        return true;
    }

    /**
     * The sea creature this catch produced, or null.
     *
     * <p>Ownership is decided by <em>where</em> the creature appeared: a sea
     * creature spawns at the spot the bobber was reeled in, so only a newly
     * arrived living entity near that spot counts as ours. The old near-player
     * radius pulled in neighbouring players' creatures at shared ponds, which
     * is why attacks sometimes hit someone else's mob. Humanoid sea creatures
     * (Banshee, Frog Man, ...) render as synthetic player entities on
     * Hypixel, so real players are filtered out through the tab list instead
     * of a blanket {@code instanceof Player} check that used to hide those
     * creatures entirely.
     *
     * <p>When several candidates are in range the nearest one wins.
     */
    private Entity findCaughtMonster(Minecraft client) {
        if (catchPosition == Vec3.ZERO) return null;
        Entity closest = null;
        double closestDistance = MONSTER_SPAWN_RANGE_SQUARED;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            if (entity instanceof ArmorStand) continue;
            if (isRealPlayer(client, entity)) continue;
            if (preCatchLivingEntities.contains(entity.getId())) continue;
            double distance = entity.position().distanceToSqr(catchPosition);
            if (distance > MONSTER_SPAWN_RANGE_SQUARED) continue;
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        return closest;
    }

    /**
     * Whether the entity is a real player (the local player or anyone listed
     * in the tab list). Hypixel renders humanoid sea creatures as fake player
     * entities that never appear in the tab list, so the player-info check is
     * what keeps them attackable while still never targeting actual players.
     */
    private static boolean isRealPlayer(Minecraft client, Entity entity) {
        if (!(entity instanceof Player player)) return false;
        if (player == client.player) return true;
        // Humanoid sea creatures carry a Hypixel-style name tag ("[LvN]" level
        // prefix or a known sea creature name). That overrides the tab list,
        // because some NPC players do show up there.
        if (looksLikeSeaCreature(player)) return false;
        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            // No server connection (singleplayer): every player entity is real.
            return true;
        }
        return connection.getPlayerInfo(player.getUUID()) != null;
    }

    /**
     * Whether the player entity's name tag marks it as a sea creature NPC.
     *
     * <p>Only the known-name list is consulted. The "[LvN]" level prefix is
     * deliberately <em>not</em> used: real SkyBlock players carry their own
     * level in front of their name tag, so it cannot tell creatures apart
     * from players. Names shorter than five letters (e.g. "Yeti") are never
     * matched freely, so ordinary player names cannot false-positive.
     */
    private static boolean looksLikeSeaCreature(Player player) {
        String plain = ChatFormatting.stripFormatting(player.getDisplayName().getString())
            .toLowerCase(Locale.ROOT);
        for (String name : KNOWN_SEA_CREATURE_NAMES) {
            if (name.length() >= 5 && plain.contains(name)) return true;
        }
        return false;
    }

    /**
     * Whether the current attack target is still close enough to fight.
     *
     * <p>The attack loop used to click blindly for as long as the target stayed
     * alive; a sea creature knocked out of range (or a player teleported away
     * by a Hyperion-style weapon) kept the loop going, so the mod right-clicked
     * into the air and drained mana for nothing. The loop now ends as soon as
     * the creature leaves the same envelope it was detected in.
     */
    private boolean isAttackTargetAround(LocalPlayer player, LivingEntity living) {
        if (living.position().distanceToSqr(player.position()) <= MONSTER_PLAYER_RANGE_SQUARED) {
            return true;
        }
        return catchPosition != Vec3.ZERO
            && living.position().distanceToSqr(catchPosition) <= MONSTER_HOOK_RANGE_SQUARED;
    }

    private boolean startAutoAttack(LocalPlayer player, Entity monster) {
        int slot = weaponSlot.intValue() - 1;
        if (slot < 0 || slot > 8) return false;
        // Fishing weapons like the Soul Whip are fishing-rod items too, so a
        // rod check here used to reject them. The only rod that must be
        // refused is the one already in hand: that slot is the fishing rod,
        // not the configured weapon.
        if (slot == player.getInventory().getSelectedSlot()) return false;
        var weapon = player.getInventory().getItem(slot);
        if (weapon.isEmpty()) return false;
        previousHotbarSlot = player.getInventory().getSelectedSlot();
        attackHotbarSlot = slot;
        attackTargetId = monster.getId();
        attackAimPoint = monster instanceof LivingEntity living ? living.getEyePosition() : monster.position();
        attackOriginalYaw = player.getYRot();
        attackOriginalPitch = player.getXRot();
        attackAimTicks = 0;
        attackTicks = 0;
        attackViewRestoreTicks = 0;
        restoringAttackView = false;
        player.getInventory().setSelectedSlot(slot);
        skillUseCooldown = 0;
        monsterDetectionTicks = 0;
        preCatchLivingEntities.clear();
        return true;
    }

    private void beginAttackViewRestore(LocalPlayer player) {
        if (previousHotbarSlot >= 0 && previousHotbarSlot <= 8) {
            player.getInventory().setSelectedSlot(previousHotbarSlot);
        }
        previousHotbarSlot = -1;
        attackHotbarSlot = -1;
        skillUseCooldown = 0;
        preCatchLivingEntities.clear();
        catchPosition = Vec3.ZERO;
        if (aimBeforeAttack.get()) {
            restoringAttackView = true;
            attackViewRestoreTicks = 0;
            return;
        }
        // The camera was never moved, so there is nothing to restore: finish
        // immediately instead of slewing the view back to a stale angle (which
        // would yank the player's own look direction after a manual fight).
        finishAttackViewRestore(player);
    }

    /** Shared tail of the attack-view restore: releases the lease and resumes fishing. */
    private void finishAttackViewRestore(LocalPlayer player) {
        rotationLease.clear();
        restoringAttackView = false;
        attackViewRestoreTicks = 0;
        clearAttackTarget();
        if (pendingCatchMovement) {
            beginMotion(player);
            pendingCatchMovement = false;
        }
    }

    private void requestAttackLook(Vec3 target) {
        rotationLease.request(target, 14.0F, 0.55F);
    }

    private void requestLookAtPoint(
        LocalPlayer player,
        Vec3 target,
        float maxDegreesPerTick,
        float smoothing
    ) {
        rotationLease.request(target, maxDegreesPerTick, smoothing);
    }

    private static boolean isLookingAt(LocalPlayer player, Vec3 target) {
        Vec3 direction = target.subtract(player.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6D) return true;
        return player.getViewVector(1.0F).normalize().dot(direction.normalize()) >= ATTACK_AIM_COSINE;
    }

    private static boolean isViewNear(LocalPlayer player, float yaw, float pitch, float tolerance) {
        return Math.abs(Mth.wrapDegrees(player.getYRot() - yaw)) <= tolerance
            && Math.abs(player.getXRot() - pitch) <= tolerance;
    }

    private void clearAttackTarget() {
        attackTargetId = -1;
        attackAimPoint = Vec3.ZERO;
        attackAimTicks = 0;
    }

    private void cancelAutoAttack(LocalPlayer player) {
        if (player != null && previousHotbarSlot >= 0 && previousHotbarSlot <= 8) {
            player.getInventory().setSelectedSlot(previousHotbarSlot);
        }
        if (player != null && aimBeforeAttack.get() && (attackTargetId >= 0 || restoringAttackView)) {
            player.setYRot(attackOriginalYaw);
            player.setXRot(attackOriginalPitch);
        }
        if (rotationLease != null) rotationLease.clear();
        previousHotbarSlot = -1;
        attackHotbarSlot = -1;
        clearAttackTarget();
        attackTicks = 0;
        restoringAttackView = false;
        attackViewRestoreTicks = 0;
        monsterDetectionTicks = 0;
        skillUseCooldown = 0;
        preCatchLivingEntities.clear();
        catchPosition = Vec3.ZERO;
        pendingCatchMovement = false;
    }

    private void reelAndQueueCast(LocalPlayer player, int delayTicks, boolean caught, Vec3 waterTarget) {
        interactions.useMainHand();
        // The reel above happens before this tick's countdown decrement, so a
        // floor of 1 would hit zero in the same tick and send a second use-item
        // packet back-to-back. A floor of 2 keeps the reel tick and the cast tick
        // distinct without changing the timing at any larger delay.
        recastCountdown = Math.max(2, delayTicks);
        castHeldTicks = 0;
        // The aim that just produced a catch is by definition aimed at water, so
        // it is the one to restore before recasting.
        castYaw = player.getYRot();
        castPitch = player.getXRot();
        castWaterTarget = findWaterTarget(player.level(), waterTarget);
        castTargetSearchCooldown = 0;
        hookAge = 0;
        noHookAge = 0;
        if (caught) beginMotion(player);
    }

    /**
     * Whether the queued cast should wait another tick.
     *
     * <p>The footwork moves the player, so the aim that caught the last fish no
     * longer points where it did 鈥?that is how a cast ended up on the bank. The
     * cast waits for the footwork to finish, slews back to the catch aim, and
     * only goes out once the look actually meets water. The hold is bounded so a
     * pond that has drained (or a spot the player has been dragged away from)
     * cannot stall the module forever.
     * <p>With View Lock disabled the camera is never touched: the cast goes out
     * as soon as the footwork finishes, wherever the player is looking.
     */
    private boolean holdCast(LocalPlayer player) {
        if (motionTick >= 0) return true;
        // With View Lock off the camera is never moved: the queued cast goes out
        // as soon as the footwork finishes, wherever the player happens to look.
        if (!viewLock.get()) return false;
        // The hold bound is checked for both remembered and remembered-less
        // cast targets: a pond that drained, a player dragged away or a look
        // that cannot reach water must not stall the module forever, so past
        // the limit the cast goes out anyway.
        if (castHeldTicks >= CAST_HOLD_LIMIT) return false;
        if (castWaterTarget != Vec3.ZERO) {
            Vec3 target = resolveCastWaterTarget(player);
            if (target == null) return true;
            requestLookAtPoint(player, target, 12.0F, 0.58F);
            if (!isViewLookingAtPoint(player, target, 2.0D)) return true;
            return !hasClearWaterLine(player, target);
        }
        // Initial casts have no remembered bobber position. Keep the user's
        // current aim, but still require an unobstructed water ray.
        requestLook(player, castYaw, castPitch);
        if (Math.abs(Mth.wrapDegrees(player.getYRot() - castYaw)) > 1.5F
            || Math.abs(player.getXRot() - castPitch) > 1.5F) return true;
        return !aimsAtWater(player);
    }

    private Vec3 resolveCastWaterTarget(LocalPlayer player) {
        Level level = player.level();
        if (castWaterTarget != Vec3.ZERO
            && castWaterTarget.distanceToSqr(player.position()) <= CAST_TARGET_RANGE_SQUARED
            && hasClearWaterLine(player, castWaterTarget)) {
            return castWaterTarget;
        }
        if (castTargetSearchCooldown > 0) {
            castTargetSearchCooldown--;
            return null;
        }
        castTargetSearchCooldown = 4;
        Vec3 nearby = findReachableWaterTarget(player, player.position(), CAST_FALLBACK_SEARCH_RADIUS);
        if (nearby != Vec3.ZERO) {
            castWaterTarget = nearby;
            return nearby;
        }
        return null;
    }

    private static Vec3 findReachableWaterTarget(LocalPlayer player, Vec3 origin, int radius) {
        Level level = player.level();
        BlockPos center = BlockPos.containing(origin);
        Vec3 best = Vec3.ZERO;
        int bestNeighbors = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getFluidState().isEmpty()) continue;
                    Vec3 candidate = Vec3.atCenterOf(pos);
                    double distance = candidate.distanceToSqr(player.position());
                    if (distance > CAST_TARGET_RANGE_SQUARED || !hasClearWaterLine(player, candidate)) continue;
                    int neighbors = waterNeighbors(level, pos);
                    if (neighbors > bestNeighbors
                        || (neighbors == bestNeighbors && distance < bestDistance)) {
                        best = candidate;
                        bestNeighbors = neighbors;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private static Vec3 findWaterTarget(Level level, Vec3 origin) {
        BlockPos center = BlockPos.containing(origin);
        Vec3 best = Vec3.ZERO;
        int bestNeighbors = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -CAST_WATER_SEARCH_RADIUS; dx <= CAST_WATER_SEARCH_RADIUS; dx++) {
            for (int dz = -CAST_WATER_SEARCH_RADIUS; dz <= CAST_WATER_SEARCH_RADIUS; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.getBlockState(pos).getFluidState().isEmpty()) continue;
                    int neighbors = waterNeighbors(level, pos);
                    Vec3 candidate = Vec3.atCenterOf(pos);
                    double distance = candidate.distanceToSqr(origin);
                    if (neighbors > bestNeighbors
                        || (neighbors == bestNeighbors && distance < bestDistance)) {
                        best = candidate;
                        bestNeighbors = neighbors;
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    private static int waterNeighbors(Level level, BlockPos pos) {
        int neighbors = 0;
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (!level.getBlockState(pos.offset(ox, 0, oz)).getFluidState().isEmpty()) neighbors++;
            }
        }
        return neighbors;
    }

    private static boolean hasClearWaterLine(LocalPlayer player, Vec3 target) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 delta = target.subtract(eye);
        double distance = delta.length();
        if (distance < 0.5D) return false;
        Vec3 direction = delta.scale(1.0D / distance);
        for (double step = 0.5D; step <= distance + 0.25D; step += 0.2D) {
            BlockPos pos = BlockPos.containing(eye.add(direction.scale(Math.min(step, distance))));
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) return true;
            if (!state.getCollisionShape(level, pos).isEmpty()) return false;
        }
        return false;
    }

    private static boolean isViewLookingAtPoint(LocalPlayer player, Vec3 target, double toleranceDegrees) {
        Vec3 direction = target.subtract(player.getEyePosition());
        if (direction.lengthSqr() < 1.0E-6D) return false;
        double cosine = Math.cos(Math.toRadians(toleranceDegrees));
        return player.getViewVector(1.0F).normalize().dot(direction.normalize()) >= cosine;
    }

    /**
     * Whether the player's look line meets water before it meets anything solid,
     * within the distance a cast bobber covers.
     */
    private static boolean aimsAtWater(LocalPlayer player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        for (double distance = 0.5D; distance <= CAST_AIM_RANGE; distance += 0.25D) {
            BlockPos position = BlockPos.containing(eye.add(look.scale(distance)));
            var state = level.getBlockState(position);
            if (!state.getFluidState().isEmpty()) return true;
            if (!state.getCollisionShape(level, position).isEmpty()) return false;
        }
        return false;
    }

    private void beginMotion(LocalPlayer player) {
        // The drift anchors to where the player is looking now, not to where they
        // looked when the module was enabled. A fixed anchor meant every catch
        // slewed the camera back to that stale orientation — up to the full lease
        // speed — the moment the user had turned away.
        originalYaw = player.getYRot();
        originalPitch = player.getXRot();
        motionAnchor = player.position();
        motionTick = 0;
        yawOffset = random.nextFloat() * 2.4F - 1.2F;
        pitchOffset = random.nextFloat() * 1.4F - 0.7F;
        planMovement(player);
    }

    /**
     * Chooses one small adjustment and, sometimes, a short return step. Several
     * unrelated legs looked erratic rather than human; an idling player usually
     * makes one brief correction and then settles again. When no direction is
     * solid ground the player simply stays put rather than hopping or stepping
     * toward water.
     */
    private void planMovement(LocalPlayer player) {
        legs.clear();
        motionLength = 8 + random.nextInt(6);
        if (!move.get()) return;

        InputAction primary = pickSafeDirection(player, null);
        if (primary == null) return;
        int start = 1 + random.nextInt(3);
        int hold = 2 + random.nextInt(3);
        legs.add(new Leg(primary, null, start, start + hold));

        int cursor = start + hold;
        if (random.nextFloat() < 0.65F) {
            cursor += 2 + random.nextInt(3);
            InputAction returnDirection = opposite(primary);
            if (isStepSafe(player, returnDirection)) {
                int returnHold = 1 + random.nextInt(2);
                legs.add(new Leg(returnDirection, null, cursor, cursor + returnHold));
                cursor += returnHold;
            }
        }
        motionLength = Math.max(motionLength, cursor + 4);
    }

    /**
     * A direction that keeps the player on solid ground, or null when none does.
     *
     * <p>{@code preferred} is tried first so a return leg mirrors the outbound
     * one when that is safe. Fishing spots are routinely a one-block ledge over
     * water or a drop, and the old code pressed a movement key without ever
     * asking what was underneath — which is how the player walked off.
     */
    private InputAction pickSafeDirection(LocalPlayer player, InputAction preferred) {
        if (preferred != null && isStepSafe(player, preferred)) return preferred;
        // Outbound adjustments prefer a sideways shuffle or one step back. A
        // forward step points toward the pond on most fishing spots and is only
        // used as the return half of a backward adjustment.
        InputAction[] candidates = preferred == null
            ? new InputAction[] {InputAction.LEFT, InputAction.RIGHT, InputAction.BACKWARD}
            : new InputAction[] {InputAction.LEFT, InputAction.RIGHT, InputAction.FORWARD, InputAction.BACKWARD};
        // Shuffled so a blocked first choice does not always fall back to the
        // same second one.
        for (int index = candidates.length - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            InputAction held = candidates[index];
            candidates[index] = candidates[swap];
            candidates[swap] = held;
        }
        for (InputAction candidate : candidates) {
            if (candidate != preferred && isStepSafe(player, candidate)) return candidate;
        }
        return null;
    }

    private static InputAction opposite(InputAction action) {
        return switch (action) {
            case LEFT -> InputAction.RIGHT;
            case RIGHT -> InputAction.LEFT;
            case FORWARD -> InputAction.BACKWARD;
            case BACKWARD -> InputAction.FORWARD;
            default -> action;
        };
    }

    /**
     * Whether one step in {@code action} lands on ground and is not walled off.
     *
     * <p>The probe is one block out in the movement direction, rotated by the
     * player's yaw. It rejects the step when the block the player would stand on
     * has no collision (air, or water at the pond's edge) and when the space
     * they would move into is blocked. A step while airborne is always refused:
     * the ground reference is meaningless mid-fall.
     */
    private boolean isStepSafe(LocalPlayer player, InputAction action) {
        return isStepSafe(player, action, null);
    }

    private boolean isStepSafe(LocalPlayer player, InputAction primary, InputAction secondary) {
        if (!player.onGround()) return false;
        Vec3 direction = directionVector(player, primary);
        if (secondary != null) {
            Vec3 side = directionVector(player, secondary);
            if (side == null) return false;
            direction = direction == null ? null : direction.add(side).normalize();
        }
        if (direction == null) return false;

        // The probe reaches as far as this tick's velocity would coast, so a
        // speed effect widens the safety margin instead of outrunning it.
        Vec3 velocity = player.getDeltaMovement();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double reach = Math.min(3.0D, STEP_PROBE_BASE + speed * STEP_COAST_TICKS);

        // Straying from the catch spot both risks the ledge and aims the next
        // cast inland, so the leash caps the probe too.
        double travelled = Math.sqrt(player.position().distanceToSqr(motionAnchor.x, player.getY(), motionAnchor.z));
        if (travelled > STEP_LEASH) {
            Vec3 homeward = new Vec3(motionAnchor.x - player.getX(), 0.0D, motionAnchor.z - player.getZ());
            // Only steps that head back toward the anchor are still allowed.
            if (homeward.lengthSqr() < 1.0E-6D || direction.dot(homeward.normalize()) < 0.2D) return false;
        }

        Level level = player.level();
        Vec3 origin = player.position();
        if (!isCorridorSafe(level, origin, direction, reach)) return false;

        // Momentum is not necessarily parallel to the newly pressed key. Check
        // its own corridor as well, otherwise a sideways drift can leave the
        // platform while the intended forward corridor remains solid.
        if (speed > 0.01D) {
            Vec3 momentumDirection = new Vec3(velocity.x, 0.0D, velocity.z).normalize();
            double momentumReach = Math.min(3.0D, 0.5D + speed * STEP_COAST_TICKS);
            if (!isCorridorSafe(level, origin, momentumDirection, momentumReach)) return false;
        }
        return true;
    }

    private static boolean isCorridorSafe(Level level, Vec3 origin, Vec3 direction, double reach) {
        if (!isFootprintSafe(level, origin)) return false;
        for (double distance = STEP_PROBE_SPACING; distance <= reach; distance += STEP_PROBE_SPACING) {
            if (!isFootprintSafe(level, origin.add(direction.scale(distance)))) return false;
        }
        return isFootprintSafe(level, origin.add(direction.scale(reach)));
    }

    /** Checks support and body clearance under the center and all footprint edges. */
    private static boolean isFootprintSafe(Level level, Vec3 point) {
        double[] offsets = {-PLAYER_FOOTPRINT_RADIUS, 0.0D, PLAYER_FOOTPRINT_RADIUS};
        for (double xOffset : offsets) {
            for (double zOffset : offsets) {
                if (!isSampleSafe(level, point.x + xOffset, point.y, point.z + zOffset)) return false;
            }
        }
        return true;
    }

    private static boolean isSampleSafe(Level level, double x, double y, double z) {
        BlockPos standingOn = BlockPos.containing(x, y - 0.2D, z);
        if (level.getBlockState(standingOn).getCollisionShape(level, standingOn).isEmpty()) return false;
        BlockPos feet = BlockPos.containing(x, y + 0.05D, z);
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()) return false;
        BlockPos head = BlockPos.containing(x, y + 1.55D, z);
        return level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    /** Unit horizontal vector for {@code action} in the player's yaw frame. */
    private static Vec3 directionVector(LocalPlayer player, InputAction action) {
        double yaw = Math.toRadians(player.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        return switch (action) {
            case FORWARD -> new Vec3(forwardX, 0.0D, forwardZ);
            case BACKWARD -> new Vec3(-forwardX, 0.0D, -forwardZ);
            case LEFT -> new Vec3(-forwardZ, 0.0D, forwardX);
            case RIGHT -> new Vec3(forwardZ, 0.0D, -forwardX);
            default -> null;
        };
    }

    private void updateMotion(LocalPlayer player) {
        if (motionTick < 0) return;
        motionTick++;
        if (rotate.get()) {
            // One small eased drift is less mechanical than several harmonics
            // moving the view independently of the player's short foot shuffle.
            float phase = Math.min(1.0F, motionTick / (float) Math.max(1, motionLength));
            float amount = (float) Math.sin(phase * Math.PI);
            requestLook(player,
                originalYaw + yawOffset * amount,
                originalPitch + pitchOffset * amount);
        }
        Leg leg = legAt(motionTick);
        if (leg == null) {
            releaseDirectionalMovement();
        } else {
            // Re-checked every tick: the ground can change mid-step, the
            // player's own momentum shifts how far the probe must look, and
            // a single bad tick of input is enough to walk off an edge.
            boolean bothSafe = isStepSafe(player, leg.primary(), leg.secondary());
            if (bothSafe) {
                pressMovement(leg.primary(), leg.secondary());
                movementSafetyTicks = STEP_COAST_TICKS;
            } else if (leg.secondary() != null && isStepSafe(player, leg.primary())) {
                // Drop the diagonal component rather than the whole step.
                pressMovement(leg.primary(), null);
                movementSafetyTicks = STEP_COAST_TICKS;
            } else {
                releaseDirectionalMovement();
            }
        }
        if (motionTick >= motionLength) {
            releaseDirectionalMovement();
            rotationLease.clear();
            motionTick = -1;
        }
    }

    /** The leg the plan wants held on {@code tick}, or null for a pause. */
    private Leg legAt(int tick) {
        for (Leg leg : legs) {
            if (tick >= leg.start() && tick < leg.end()) return leg;
        }
        return null;
    }

    private void pressMovement(InputAction primary, InputAction secondary) {
        releaseDirectionalMovement();
        leaseFor(primary).setPressed(true);
        if (secondary != null) leaseFor(secondary).setPressed(true);
    }

    private InputLease leaseFor(InputAction action) {
        return switch (action) {
            case FORWARD -> forwardLease;
            case BACKWARD -> backwardLease;
            case LEFT -> leftLease;
            default -> rightLease;
        };
    }

    private void requestLook(LocalPlayer player, float yaw, float pitch) {
        requestLook(player, yaw, pitch, 4.5F, 0.78F);
    }

    private void requestLook(
        LocalPlayer player,
        float yaw,
        float pitch,
        float maxDegreesPerTick,
        float smoothing
    ) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        Vec3 target = player.getEyePosition().add(
            -Math.sin(yawRadians) * horizontal * 8.0D,
            -Math.sin(pitchRadians) * 8.0D,
            Math.cos(yawRadians) * horizontal * 8.0D
        );
        rotationLease.request(target, maxDegreesPerTick, smoothing);
    }

    private void releaseMovement() {
        releaseDirectionalMovement();
        if (sneakLease != null) sneakLease.setPressed(false);
        if (jumpLease != null) jumpLease.setPressed(false);
        if (rotationLease != null) rotationLease.clear();
    }

    private void releaseDirectionalMovement() {
        if (forwardLease != null) forwardLease.setPressed(false);
        if (backwardLease != null) backwardLease.setPressed(false);
        if (leftLease != null) leftLease.setPressed(false);
        if (rightLease != null) rightLease.setPressed(false);
    }

    private void resetState() {
        tick = 0L;
        hookAge = 0;
        noHookAge = 0;
        recastCountdown = -1;
        motionTick = -1;
        legs.clear();
        castHeldTicks = 0;
        castTargetSearchCooldown = 0;
        motionAnchor = Vec3.ZERO;
        castWaterTarget = Vec3.ZERO;
        monsterDetectionTicks = 0;
        skillUseCooldown = 0;
        previousHotbarSlot = -1;
        attackHotbarSlot = -1;
        attackTargetId = -1;
        attackAimPoint = Vec3.ZERO;
        attackAimTicks = 0;
        attackTicks = 0;
        attackViewRestoreTicks = 0;
        restoringAttackView = false;
        preCatchLivingEntities.clear();
        catchPosition = Vec3.ZERO;
        pendingCatchMovement = false;
        movementSafetyTicks = 0;
        biteLatched = false;
        stuckStreak = 0;
        lastBiteTick = Long.MIN_VALUE / 2L;
        lastIdleCastTick = Long.MIN_VALUE / 2L;
        reeledMarkerId = -1;
    }

    static FishingHook ownedHook(Minecraft client, LocalPlayer player) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof FishingHook hook && hook.getPlayerOwner() == player) return hook;
        }
        return null;
    }

    /**
     * The bite marker closest to the hook, or {@code null} when none is in
     * range. The nearest one wins so a neighbour's marker cannot be picked over
     * your own when both are near the boundary.
     */
    static Entity biteMarkerNear(Minecraft client, FishingHook hook) {
        Entity closest = null;
        double closestDistance = BITE_MARKER_RADIUS_SQUARED;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!entity.hasCustomName()
                || entity.getCustomName() == null
                || !entity.getCustomName().getString().contains("!!!")) {
                continue;
            }
            double distance = entity.distanceToSqr(hook);
            if (distance <= closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        return closest;
    }
}
