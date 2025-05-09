package pokecube.adventures.utils.trade_presets;

import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import pokecube.adventures.capabilities.utils.TypeTrainer.TrainerTrade;
import pokecube.adventures.capabilities.utils.TypeTrainer.TrainerTrades;
import pokecube.adventures.utils.TradeEntryLoader;
import pokecube.adventures.utils.TradeEntryLoader.Trade;
import pokecube.adventures.utils.TradeEntryLoader.TradePreset;
import pokecube.api.utils.Tools;
import pokecube.core.items.berries.BerryManager;
import pokecube.core.items.berries.ItemBerry;

@TradePresetAn(key = "allBerryBuy")
public class BuyRandomBerry implements TradePreset
{
    @Override
    public void apply(final Trade trade, final TrainerTrades trades)
    {
        for (RegistryObject<ItemBerry> berry : BerryManager.berryItems.values())
        {
            final ItemStack badge = new ItemStack(berry.get());
            if (!badge.isEmpty())
            {
                if (trade.count > 0) badge.setCount(trade.count);
                Map<String, String> values = trade.sell.getValues();
                TrainerTrade recipe;
                final ItemStack sell = Tools.getStack(values);
                recipe = new TrainerTrade(badge, ItemStack.EMPTY, sell, trade);
                values = trade.values;
                if (values.containsKey(TradeEntryLoader.CHANCE))
                    recipe.chance = Float.parseFloat(values.get(TradeEntryLoader.CHANCE));
                if (values.containsKey(TradeEntryLoader.MIN))
                    recipe.min = Integer.parseInt(values.get(TradeEntryLoader.MIN));
                if (values.containsKey(TradeEntryLoader.MAX))
                    recipe.max = Integer.parseInt(values.get(TradeEntryLoader.MAX));
                trades.tradesList.add(recipe);
            }
        }
    }

}
