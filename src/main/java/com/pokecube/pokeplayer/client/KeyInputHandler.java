package com.pokecube.pokeplayer.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pokecube.pokeplayer.client.container.MachineSlotMenu;
import com.pokecube.pokeplayer.data.PokePlayerDataHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.List;
import pokecube.api.PokecubeAPI;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.moves.MoveEntry;
import pokecube.core.PokecubeCore;
import pokecube.core.moves.MovesUtils;
import thut.api.maths.Vector3;

import java.util.Map;

// @Mod.EventBusSubscriber(modid = "pokecube_expansion", value = Dist.CLIENT)
public class KeyInputHandler {
    private static int selectedMove = 0;
    private static final int MAX_MOVES = 4;
    static Map<String, String> guistate = MachineSlotMenu.guistate;

private static LivingEntity findClosestEntity(Vector3 position, Level level) {
    // Define a bounding box around the given position to find nearby entities
    AABB searchArea = position.getAABB().inflate(1.0); // 1-block radius, adjust as needed
    // Get all LivingEntity instances within the bounding box
    List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);

    // Return the first entity found, or null if no entities were found
    return entities.isEmpty() ? null : entities.get(0);
}

    public static void handleKeyPress(int key){
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null) return;

        switch (key){
            case 0 -> changeMove(1);
            case 1 -> changeMove(-1);
            case 2 -> useSelectedMove();
        }
    }

    public static void changeMove(int direction){
        selectedMove = (selectedMove + direction + MAX_MOVES) % MAX_MOVES;
        System.out.println("Movimento selecionado: " + guistate.get("move_" + selectedMove));
    }

    private static void useSelectedMove() {
    Player player = Minecraft.getInstance().player;
    if (guistate.containsKey("move_" + selectedMove)) {
        String moveName = guistate.get("move_" + selectedMove);
        MoveEntry move = MovesUtils.getMove(moveName);

        if (move != null) {
            PokecubeAPI.LOGGER.info("Executing move: " + moveName);
            IPokemob pokemob = PokePlayerDataHandler.getInstance().getPokemobForPlayer(player);

            Vector3 start = new Vector3().set(player.position());
            Vector3 end = findTarget(); // Get the target position (Vector3)

            if (end != null) {
                // Find the closest LivingEntity to the end position
                LivingEntity targetEntity = findClosestEntity(end, player.level);

                // Use the move, handling both entity targeting and positional effects
                if (targetEntity != null) {
                    MovesUtils.useMove(move, pokemob.getEntity(), targetEntity, start, end);
                    PokecubeAPI.LOGGER.info("Move executed: " + moveName + " targeting entity " + targetEntity.getName().getString());
                } else {
                    System.out.println("No target entity found. Applying positional effects.");
                    MovesUtils.useMove(move, pokemob.getEntity(), null, start, end);
                }
            } else {
                System.out.println("No valid target found for the move.");
            }
        }
    }
}

    private static Vector3 findTarget() {
        Player player = Minecraft.getInstance().player;
        ClipContext context = new ClipContext(
                player.getEyePosition(1.0F),
                player.getEyePosition(1.0F).add(player.getLookAngle().scale(10.0D)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );
        HitResult result = player.level.clip(context);

        if (result.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityResult = (EntityHitResult) result;
            if (entityResult.getEntity() instanceof LivingEntity) {
                return new Vector3().set(entityResult.getEntity());
            }
        }
        return null;
    }
}
