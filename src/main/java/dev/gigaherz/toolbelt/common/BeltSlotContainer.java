package dev.gigaherz.toolbelt.common;

import com.google.common.collect.Lists;
import dev.gigaherz.toolbelt.BeltFinder;
import dev.gigaherz.toolbelt.ToolBelt;
import dev.gigaherz.toolbelt.customslots.ExtensionSlotSlot;
import dev.gigaherz.toolbelt.customslots.IExtensionSlot;
import dev.gigaherz.toolbelt.network.ContainerSlotsHack;
import dev.gigaherz.toolbelt.slot.BeltExtensionSlot;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.util.RecipeBookCategories;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeBookCategory;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.util.List;

public class BeltSlotContainer extends RecipeBookContainer<CraftingInventory>
{
    @ObjectHolder("toolbelt:belt_slot_container")
    public static ContainerType<BeltSlotContainer> TYPE;

    public static final ResourceLocation SLOT_BACKGROUND = ToolBelt.location("gui/empty_belt_slot_background");

    private final ExtensionSlotSlot slotBelt;
    private final IExtensionSlot extensionSlot;

    private final CraftingInventory craftingInventory = new CraftingInventory(this, 2, 2);
    private final CraftResultInventory craftResultInventory = new CraftResultInventory();
    public final boolean isLocalWorld;
    private final PlayerEntity player;

    private interface SlotFactory<T extends Slot>
    {
        T create(IExtensionSlot slot, int x, int y);
    }

    private SlotFactory<ExtensionSlotSlot> slotFactory = DistExecutor.runForDist(
            () -> () -> ExtensionSlotSlotClient::new,
            () -> () -> ExtensionSlotSlot::new
    );

    public BeltSlotContainer(int id, PlayerInventory playerInventory)
    {
        this(id, playerInventory, true);
    }

    public BeltSlotContainer(int id, PlayerInventory playerInventory, boolean localWorld)
    {
        super(TYPE, id);
        this.isLocalWorld = localWorld;
        this.player = playerInventory.player;
        this.addSlot(new CraftingResultSlot(playerInventory.player, this.craftingInventory, this.craftResultInventory, 0, 154, 28));

        for (int i = 0; i < 2; ++i)
        {
            for (int j = 0; j < 2; ++j)
            {
                this.addSlot(new Slot(this.craftingInventory, j + i * 2, 98 + j * 18, 18 + i * 18));
            }
        }

        for (int k = 0; k < 4; ++k)
        {
            final EquipmentSlotType equipmentslottype = PlayerContainer.SLOT_IDS[k];
            this.addSlot(new Slot(playerInventory, 39 - k, 8, 8 + k * 18)
            {
                /**
                 * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in
                 * the case of armor slots)
                 */
                public int getMaxStackSize()
                {
                    return 1;
                }

                /**
                 * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
                 */
                public boolean mayPlace(ItemStack stack)
                {
                    return stack.canEquip(equipmentslottype, player);
                }

                /**
                 * Return whether this slot's stack can be taken from this slot.
                 */
                public boolean mayPickup(PlayerEntity playerIn)
                {
                    ItemStack itemstack = this.getItem();
                    return !itemstack.isEmpty() && !playerIn.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false : super.mayPickup(playerIn);
                }

                @Nullable
                @OnlyIn(Dist.CLIENT)
                public String getSlotTexture()
                {
                    return PlayerContainer.TEXTURE_EMPTY_SLOTS[equipmentslottype.getIndex()].toString();
                }
            });
        }

        for (int l = 0; l < 3; ++l)
        {
            for (int j1 = 0; j1 < 9; ++j1)
            {
                this.addSlot(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1)
        {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        }

        this.addSlot(new Slot(playerInventory, 40, 77, 62)
        {
            @Nullable
            @OnlyIn(Dist.CLIENT)
            public String getSlotTexture()
            {
                return "item/empty_armor_slot_shield";
            }
        });

        BeltExtensionSlot container = playerInventory.player.getCapability(BeltExtensionSlot.CAPABILITY)
                .orElseThrow(() -> new RuntimeException("Item handler not present."));

        extensionSlot = container.getBelt();

        this.addSlot(slotBelt = slotFactory.create(BeltSlotContainer.this.extensionSlot, 77, 44));

        if (!localWorld)
        {
            ToolBelt.channel.sendToServer(new ContainerSlotsHack());
        }
    }

    @Override
    public List<RecipeBookCategories> getRecipeBookCategories()
    {
        return Lists.newArrayList(RecipeBookCategories.CRAFTING_SEARCH, RecipeBookCategories.CRAFTING_EQUIPMENT, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS, RecipeBookCategories.CRAFTING_MISC, RecipeBookCategories.CRAFTING_REDSTONE);
    }

    @Override
    public RecipeBookCategory getRecipeBookType()
    {
        return RecipeBookCategory.CRAFTING;
    }

    @Override
    public void fillCraftSlotsStackedContents(RecipeItemHelper itemHelperIn)
    {
        this.craftingInventory.fillStackedContents(itemHelperIn);
    }

    @Override
    public void clearCraftingContent()
    {
        this.craftResultInventory.clearContent();
        this.craftingInventory.clearContent();
    }

    @Override
    public boolean recipeMatches(IRecipe<? super CraftingInventory> recipeIn)
    {
        return recipeIn.matches(this.craftingInventory, this.player.level);
    }

    @Override
    public void slotsChanged(IInventory inventoryIn)
    {
        WorkbenchContainer.slotChangedCraftingGrid(this.containerId, this.player.level, this.player, this.craftingInventory, this.craftResultInventory);
    }

    @Override
    public void removed(PlayerEntity playerIn)
    {
        super.removed(playerIn);

        this.craftResultInventory.clearContent();

        if (!playerIn.level.isClientSide)
        {
            this.clearContainer(playerIn, playerIn.level, this.craftingInventory);
            BeltFinder.sendSync(playerIn);
        }
    }

    @Override
    public boolean stillValid(PlayerEntity playerIn)
    {
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn)
    {
        return slotIn.container != this.craftResultInventory && super.canTakeItemForPickAll(stack, slotIn);
    }

    @Override
    public int getResultSlotIndex()
    {
        return 0;
    }

    @Override
    public int getGridWidth()
    {
        return this.craftingInventory.getWidth();
    }

    @Override
    public int getGridHeight()
    {
        return this.craftingInventory.getHeight();
    }

    @Override
    public int getSize()
    {
        return 5;
    }

    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity playerIn, int index)
    {
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem())
        {
            ItemStack remaining = ItemStack.EMPTY;
            ItemStack slotContents = slot.getItem();
            remaining = slotContents.copy();

            if (index == slotBelt.index)
            {
                if (!this.moveItemStackTo(slotContents, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }

                return remaining;
            }
            else if (slot.mayPlace(slotContents))
            {
                if (!this.moveItemStackTo(slotContents, slotBelt.index, slotBelt.index + 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
        }

        ItemStack itemstack = ItemStack.EMPTY;
        if (slot != null && slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            EquipmentSlotType equipmentslottype = MobEntity.getEquipmentSlotForItem(itemstack);
            if (index == 0)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, true))
                {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else if (index >= 1 && index < 5)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 5 && index < 9)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (equipmentslottype.getType() == EquipmentSlotType.Group.ARMOR && !this.slots.get(8 - equipmentslottype.getIndex()).hasItem())
            {
                int i = 8 - equipmentslottype.getIndex();
                if (!this.moveItemStackTo(itemstack1, i, i + 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (equipmentslottype == EquipmentSlotType.OFFHAND && !this.slots.get(45).hasItem())
            {
                if (!this.moveItemStackTo(itemstack1, 45, 46, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 9 && index < 36)
            {
                if (!this.moveItemStackTo(itemstack1, 36, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 36 && index < 45)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 36, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(itemstack1, 9, 45, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = slot.onTake(playerIn, itemstack1);
            if (index == 0)
            {
                playerIn.drop(itemstack2, false);
            }
        }

        return itemstack;
    }

    private class ExtensionSlotSlotClient extends ExtensionSlotSlot
    {
        {
            setBackground(AtlasTexture.LOCATION_BLOCKS, SLOT_BACKGROUND);
        }

        public ExtensionSlotSlotClient(IExtensionSlot slot, int x, int y)
        {
            super(slot, x, y);
        }
    }
}