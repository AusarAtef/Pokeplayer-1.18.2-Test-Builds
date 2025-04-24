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
import net.minecraft.world.phys.Vec3d;
import pokecube.api.PokecubeAPI;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.moves.MoveEntry;
import pokecube.core.PokecubeCore;
import pokecube.core.moves.MovesUtils;
import pokecube.core.utils.Vector3;

import java.util.Map;

//@Mod.EventBusSubscriber(modid = "pokeplayer_machine", value = Dist.CLIENT)
public class KeyInputHandler {
    private static int selectedMove = 0;
    private static final int MAX_MOVES = 4;
    static Map<String, String> guistate = MachineSlotMenu.guistate;

    public static void handleKeyPress(int key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        switch (key) {
            case 0 -> changeMove(1);
            case 1 -> changeMove(-1);
            case 2 -> useSelectedMove();
        }
    }

    public static void changeMove(int direction) {
        selectedMove = (selectedMove + direction + MAX_MOVES) % MAX_MOVES;
        System.out.println("Movimento selecionado: " + guistate.get("move_" + selectedMove));
    }

    private static void useSelectedMove() {
        Player player = Minecraft.getInstance().player;
        if (guistate.containsKey("move_" + selectedMove)) {
            String moveName = guistate.get("move_" + selectedMove);
            MoveEntry move = MovesUtils.getMove(moveName);

            if (move != null) {
                PokecubeAPI.LOGGER.info("Executando movimento: " + moveName);
                IPokemob pokemob = PokePlayerDataHandler.getInstance().getPokemobForPlayer(player);

                Vec3d start = player.position();
                Vec3d end = findTarget();

                if (end != null) {
                    Vector3 pokeStart = new Vector3().set(start.x, start.y, start.z);
                    Vector3 pokeEnd = new Vector3().set(end.x, end.y, end.z);
                    MovesUtils.useMove(move, pokemob.getEntity(), pokeEnd, pokeStart, pokeEnd);
                    PokecubeAPI.LOGGER.info("Executando movimento: " + moveName + " pelo Pokémob " + pokemob.getPokedexEntry().getName());
                } else {
                    System.out.println("Nenhum alvo válido encontrado para o movimento.");
                }
            }
        }
    }

    private static Vec3d findTarget() {
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
                return entityResult.getEntity().position();
            }
        }
        return null;
    }
}