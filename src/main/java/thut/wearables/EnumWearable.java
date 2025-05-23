package thut.wearables;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public enum EnumWearable
{

    FINGER(2, 0), WRIST(2, 2), ANKLE(2, 4), NECK(6), BACK(7), WAIST(8), EAR(2, 9), EYE(11), HAT(12);

    static EnumWearable[] BYINDEX = new EnumWearable[13];
    static Set<IWearableChecker> checkers = Sets.newHashSet();
    static
    {
        EnumWearable.BYINDEX[0] = FINGER;
        EnumWearable.BYINDEX[1] = FINGER;
        EnumWearable.BYINDEX[2] = WRIST;
        EnumWearable.BYINDEX[3] = WRIST;
        EnumWearable.BYINDEX[4] = ANKLE;
        EnumWearable.BYINDEX[5] = ANKLE;
        EnumWearable.BYINDEX[6] = NECK;
        EnumWearable.BYINDEX[7] = BACK;
        EnumWearable.BYINDEX[8] = WAIST;
        EnumWearable.BYINDEX[9] = EAR;
        EnumWearable.BYINDEX[10] = EAR;
        EnumWearable.BYINDEX[11] = EYE;
        EnumWearable.BYINDEX[12] = HAT;

        EnumWearable.checkers.add(new DefaultChecker());
    }

    public static final Map<String, EnumWearable> wearableNames = Maps.newHashMap();
    public static final Map<String, Integer> slotsNames = Maps.newHashMap();

    static
    {
        for (final EnumWearable w : EnumWearable.values()) if (w.slots == 2)
        {
            wearableNames.put("__" + w + "_right__", w);
            slotsNames.put("__" + w + "_right__", 0);
            wearableNames.put("__" + w + "_left__", w);
            slotsNames.put("__" + w + "_left__", 1);
        }
        else
        {
            wearableNames.put("__" + w + "__", w);
            slotsNames.put("__" + w + "__", 0);
        }
    }

    public static boolean canTakeOff(final LivingEntity wearer, final ItemStack stack, final int index)
    {
        if (stack.isEmpty()) return true;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        for (final IWearableChecker checker : EnumWearable.checkers)
            if (!checker.canRemove(wearer, stack, slot, subIndex)) return false;
        return true;
    }

    public static String getIcon(final int index)
    {
        String tex = null;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        switch (slot)
        {
        case ANKLE:
            tex = ThutWearables.MODID + ":item/empty_ankle_" + (subIndex == 0 ? "left" : "right");
            break;
        case BACK:
            tex = ThutWearables.MODID + ":item/empty_back";
            break;
        case EAR:
            tex = ThutWearables.MODID + ":item/empty_ear_" + (subIndex == 0 ? "left" : "right");
            break;
        case EYE:
            tex = ThutWearables.MODID + ":item/empty_eye";
            break;
        case FINGER:
            tex = ThutWearables.MODID + ":item/empty_finger_" + (subIndex == 0 ? "left" : "right");
            break;
        case HAT:
            tex = ThutWearables.MODID + ":item/empty_hat";
            break;
        case NECK:
            tex = ThutWearables.MODID + ":item/empty_neck";
            break;
        case WAIST:
            tex = ThutWearables.MODID + ":item/empty_waist";
            break;
        case WRIST:
            tex = ThutWearables.MODID + ":item/empty_wrist_" + (subIndex == 0 ? "left" : "right");
            break;
        default:
            break;
        }
        return tex;
    }

    public static EnumWearable getSlot(final ItemStack item)
    {
        if (item.isEmpty()) return null;
        for (final IWearableChecker checker : EnumWearable.checkers)
        {
            final EnumWearable ret = checker.getSlot(item);
            if (ret != null) return ret;
        }
        return null;
    }

    public static int getSubIndex(final int index)
    {
        return index - EnumWearable.BYINDEX[index].index;
    }

    public static EnumWearable getWearable(final int index)
    {
        return EnumWearable.BYINDEX[index];
    }

    public static void interact(final Player player, final ItemStack item, final int index, final UseOnContext context)
    {
        if (item.isEmpty()) return;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        for (final IWearableChecker checker : EnumWearable.checkers)
            checker.onInteract(player, item, slot, subIndex, context);
    }

    public static void putOn(final LivingEntity wearer, final ItemStack stack, final int index)
    {
        if (stack.isEmpty()) return;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        for (final IWearableChecker checker : EnumWearable.checkers) checker.onPutOn(wearer, stack, slot, subIndex);
    }

    public static void registerWearableChecker(final IWearableChecker checker)
    {
        EnumWearable.checkers.add(checker);
    }

    public static void takeOff(final LivingEntity wearer, final ItemStack stack, final int index)
    {
        if (stack.isEmpty()) return;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        for (final IWearableChecker checker : EnumWearable.checkers) checker.onTakeOff(wearer, stack, slot, subIndex);
    }

    public static void tick(final LivingEntity wearer, final ItemStack stack, final int index)
    {
        if (stack.isEmpty()) return;
        final EnumWearable slot = EnumWearable.getWearable(index);
        final int subIndex = EnumWearable.getSubIndex(index);
        for (final IWearableChecker checker : EnumWearable.checkers) checker.onUpdate(wearer, stack, slot, subIndex);
    }

    public final int slots;

    public final int index;

    private EnumWearable(final int index)
    {
        this.index = index;
        this.slots = 1;
    }

    private EnumWearable(final int slots, final int index)
    {
        this.index = index;
        this.slots = slots;
    }
}
