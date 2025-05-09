package pokecube.legends.conditions.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import pokecube.api.PokecubeAPI;
import pokecube.core.database.resources.PackFinder;
import pokecube.legends.conditions.data.Conditions.EntriedCondition;
import pokecube.legends.conditions.data.Conditions.PresetCondition;
import pokecube.legends.conditions.data.Conditions.TypedCondition;
import pokecube.legends.spawns.LegendarySpawn;
import thut.api.data.DataHelpers;
import thut.api.data.DataHelpers.ResourceData;
import thut.api.util.JsonUtil;
import thut.lib.ResourceHelper;

public class ConditionLoader extends ResourceData
{

    public static Map<String, Class<? extends PresetCondition>> __presets__ = Maps.newHashMap();

    static
    {
        ConditionLoader.__presets__.put("entry_based", EntriedCondition.class);
        ConditionLoader.__presets__.put("type_based", TypedCondition.class);
        ConditionLoader.__presets__.put("spawns_only", PresetCondition.class);
    }

    private final String tagPath;

    public boolean validLoad = false;

    public ConditionLoader(final String string)
    {
        super(string);
        this.tagPath = string;
        DataHelpers.addDataType(this);
    }

    List<PresetCondition> conditions = Lists.newArrayList();

    @Override
    public void reload(final AtomicBoolean valid)
    {
        this.validLoad = false;
        final String path = new ResourceLocation(this.tagPath).getPath();
        final Map<ResourceLocation, Resource> resources = PackFinder.getJsonResources(path);
        this.validLoad = !resources.isEmpty();
        this.conditions.clear();
        LegendarySpawn.data_spawns.clear();
        this.preLoad();
        resources.forEach((l, r) -> this.loadFile(l, r));
        if (this.validLoad) valid.set(true);
    }

    @Override
    public void postReload()
    {
        LegendarySpawn.data_spawns.clear();
        this.conditions.forEach(c -> c.register());
        this.conditions.clear();
    }

    private void loadFile(final ResourceLocation l, Resource r)
    {
        try
        {
            final List<Conditions> loaded = Lists.newArrayList();

            // This one we just take the first resourcelocation. If someone
            // wants to edit an existing one, it means they are most likely
            // trying to remove default behaviour. They can add new things by
            // just adding another json file to the correct package.
            final BufferedReader reader = ResourceHelper.getReader(r);
            if (reader == null) throw new FileNotFoundException(l.toString());
            try
            {
                final Conditions temp = JsonUtil.gson.fromJson(reader, Conditions.class);
                if (!confirmNew(temp, l))
                {
                    reader.close();
                    return;
                }
                if (temp.replace) loaded.clear();
                loaded.add(temp);
            }
            catch (final Exception e)
            {
                // Might not be valid, so log and skip in that case.
                PokecubeAPI.LOGGER.error("Malformed Json for Mutations in {}", l);
                PokecubeAPI.LOGGER.error(e);
            }
            reader.close();

            for (final Conditions m : loaded)
            {
                final List<PresetCondition> conds = m.conditions;
                for (final PresetCondition cond : conds)
                {
                    final String preset = cond.preset;
                    final Class<? extends PresetCondition> preset_class = ConditionLoader.__presets__.get(preset);
                    if (preset_class == null)
                    {
                        PokecubeAPI.LOGGER.error("No preset found for {}", preset);
                        continue;
                    }
                    try
                    {
                        final PresetCondition actual = preset_class.getConstructor().newInstance();
                        actual.name = cond.name;
                        actual.options = cond.options;
                        actual.preset = cond.preset;
                        actual.spawn = cond.spawn;
                        this.conditions.add(actual);
                    }
                    catch (final Exception e)
                    {
                        // Might not be valid, so log and skip in that case.
                        PokecubeAPI.LOGGER.error("Error processing a preset in {}", l);
                        PokecubeAPI.LOGGER.error(e);
                    }
                }
            }
        }
        catch (final Exception e)
        {
            // Might not be valid, so log and skip in that case.
            PokecubeAPI.LOGGER.error("Error with resources in {}", l);
            PokecubeAPI.LOGGER.error(e);
        }

    }
}
