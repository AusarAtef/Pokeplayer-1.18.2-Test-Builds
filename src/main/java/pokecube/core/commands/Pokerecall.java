package pokecube.core.commands;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import pokecube.api.entity.pokemob.IPokemob;
import pokecube.api.entity.pokemob.PokemobCaps;
import pokecube.api.entity.pokemob.ai.GeneralStates;
import pokecube.api.entity.pokemob.ai.LogicStates;
import pokecube.core.PokecubeCore;
import pokecube.core.entity.pokecubes.EntityPokecubeBase;
import pokecube.core.eventhandlers.EventsHandler;
import pokecube.core.eventhandlers.PCEventsHandler;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.items.pokecubes.helper.SendOutManager;
import pokecube.core.utils.PokemobTracker;
import thut.api.util.PermNodes;
import thut.api.util.PermNodes.DefaultPermissionLevel;
import thut.core.common.commands.CommandTools;
import thut.lib.TComponent;

public class Pokerecall
{
    private static SuggestionProvider<CommandSourceStack> SUGGEST_NAMES = (ctx, sb) -> {
        final ServerPlayer player = ctx.getSource().getPlayerOrException();
        final Set<String> opts = Sets.newHashSet();
        final List<Entity> mobs = PokemobTracker.getMobs(player,
                c -> EventsHandler.validRecall(player, c, null, true, true));
        for (final Entity e : mobs)
        {
            final IPokemob poke = PokemobCaps.getPokemobFor(e);
            if (poke != null) opts.add(e.getDisplayName().getString());
            else if (e instanceof EntityPokecubeBase cube)
            {
                final Entity mob = PokecubeManager.itemToMob(cube.getItem(), cube.getLevel());
                if (mob != null) opts.add(mob.getDisplayName().getString());
            }
        }
        return net.minecraft.commands.SharedSuggestionProvider.suggest(opts, sb);
    };

    public static int execute(final CommandSourceStack source, final String pokemob) throws CommandSyntaxException
    {
        int num = 0;
        final ServerPlayer player = source.getPlayerOrException();
        for (final Entity e : PCEventsHandler.getOutMobs(player, true))
            if (e.getDisplayName().getString().equals(pokemob))
        {
            final IPokemob poke = PokemobCaps.getPokemobFor(e);
            if (poke != null)
            {
                poke.onRecall();
                num++;
            }
        }
            else if (e instanceof EntityPokecubeBase cube)
        {
            final Entity mob = PokecubeManager.itemToMob(cube.getItem(), cube.getLevel());
            if (mob != null && mob.getDisplayName().getString().equals(pokemob))
            {
                final LivingEntity sent = SendOutManager.sendOut(cube, true, false);
                IPokemob poke;
                if (sent != null && (poke = PokemobCaps.getPokemobFor(sent)) != null)
                {
                    poke.onRecall();
                    num++;
                }
            }
        }
        if (num == 0) source.sendSuccess(TComponent.translatable("pokecube.recall.fail"), false);
        else source.sendSuccess(TComponent.translatable("pokecube.recall.success", num), false);
        return 0;
    }

    public static int execute(final CommandSourceStack source, final ServerPlayer player, final boolean all,
            final boolean sitting, final boolean staying) throws CommandSyntaxException
    {
        int num = 0;
        for (final Entity e : PCEventsHandler.getOutMobs(player, true))
        {
            IPokemob poke = PokemobCaps.getPokemobFor(e);
            if (poke != null) if (all || sitting && poke.getLogicState(LogicStates.SITTING)
                    || staying && poke.getGeneralState(GeneralStates.STAYING))
            {
                poke.onRecall();
                num++;
            }
            else if (e instanceof EntityPokecubeBase cube)
            {
                final LivingEntity sent = SendOutManager.sendOut(cube, true);
                if (sent != null && (poke = PokemobCaps.getPokemobFor(e)) != null)
                {
                    poke.onRecall();
                    num++;
                }
            }
        }
        if (num == 0) source.sendSuccess(TComponent.translatable("pokecube.recall.fail"), false);
        else source.sendSuccess(TComponent.translatable("pokecube.recall.success", num), false);
        return 0;
    }

    public static void register(final CommandDispatcher<CommandSourceStack> commandDispatcher)
    {
        PermNodes.registerBooleanNode(PokecubeCore.MODID, "command.pokerecall", DefaultPermissionLevel.ALL,
                "Is the player allowed to use /pokerecall");
        PermNodes.registerBooleanNode(PokecubeCore.MODID, "command.pokerecall.other", DefaultPermissionLevel.OP,
                "Is the player allowed to use /pokerecall to recall other people's mobs");

        final Predicate<CommandSourceStack> op_perm = cs -> CommandTools.hasPerm(cs, "command.pokerecall.other");

        // Setup with name and permission
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("pokerecall")
                .requires(cs -> CommandTools.hasPerm(cs, "command.pokerecall"));

        // No target argument version
        command = command.then(Commands.argument("name", StringArgumentType.string()).suggests(Pokerecall.SUGGEST_NAMES)
                .executes(ctx -> Pokerecall.execute(ctx.getSource(), StringArgumentType.getString(ctx, "name"))));
        // Target argument version
        command = command.then(Commands.literal("all").executes(
                ctx -> Pokerecall.execute(ctx.getSource(), ctx.getSource().getPlayerOrException(), true, true, true)));
        command = command.then(Commands.literal("sitting").executes(ctx -> Pokerecall.execute(ctx.getSource(),
                ctx.getSource().getPlayerOrException(), false, true, false)));
        command = command.then(Commands.literal("staying").executes(ctx -> Pokerecall.execute(ctx.getSource(),
                ctx.getSource().getPlayerOrException(), false, false, true)));

        // Target with player
        command = command.then(Commands.literal("all")
                .then(Commands.argument("owner", EntityArgument.player()).requires(op_perm).executes(ctx -> Pokerecall
                        .execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "owner"), true, true, true))));
        command = command.then(Commands.literal("sitting")
                .then(Commands.argument("owner", EntityArgument.player()).requires(op_perm).executes(ctx -> Pokerecall
                        .execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "owner"), false, true, true))));
        command = command.then(Commands.literal("staying")
                .then(Commands.argument("owner", EntityArgument.player()).requires(op_perm).executes(ctx -> Pokerecall
                        .execute(ctx.getSource(), EntityArgument.getPlayer(ctx, "owner"), false, false, true))));
        commandDispatcher.register(command);
    }
}
