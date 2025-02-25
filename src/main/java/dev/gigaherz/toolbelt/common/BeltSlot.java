package dev.gigaherz.toolbelt.common;

import dev.gigaherz.toolbelt.ConfigData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class BeltSlot extends Slot
{
    public BeltSlot(IInventory playerInventory, ItemStack heldItem, int blockedSlot, int index, int xPosition, int yPosition)
    {
        super(new IInventory()
        {
            final IInventory sourceInventory = playerInventory;
            final int slot = blockedSlot;
            final int subSlot = index;
            final ItemStack fallbackItem = heldItem;

            ItemStack beltStack = null;
            IItemHandlerModifiable inventory = null;

            IItemHandlerModifiable findStack()
            {
                ItemStack stack = slot >= 0 ? sourceInventory.getItem(slot) : fallbackItem;
                if (stack != beltStack)
                {
                    beltStack = stack;
                    inventory = (IItemHandlerModifiable) (
                            stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)
                                    .orElseThrow(() -> new RuntimeException("No inventory!"))
                    );
                }
                return inventory;
            }

            @Override
            public int getContainerSize()
            {
                return 1;
            }

            @Override
            public boolean isEmpty()
            {
                return getItem(0).getCount() <= 0;
            }

            @Override
            public ItemStack getItem(int n)
            {
                return findStack().getStackInSlot(subSlot);
            }

            @Override
            public ItemStack removeItem(int n, int count)
            {
                return findStack().extractItem(subSlot, count, false);
            }

            @Override
            public ItemStack removeItemNoUpdate(int n)
            {
                ItemStack existing = getItem(0);
                setItem(n, ItemStack.EMPTY);
                return existing;
            }

            @Override
            public void setItem(int n, ItemStack stack)
            {
                findStack().setStackInSlot(subSlot, stack);
            }

            @Override
            public int getMaxStackSize()
            {
                return findStack().getSlotLimit(subSlot);
            }

            @Override
            public void setChanged()
            {

            }

            @Override
            public boolean stillValid(PlayerEntity player)
            {
                return false;
            }

            @Override
            public void startOpen(PlayerEntity player)
            {

            }

            @Override
            public void stopOpen(PlayerEntity player)
            {

            }

            @Override
            public boolean canPlaceItem(int index, ItemStack stack)
            {
                return ConfigData.isItemStackAllowed(stack);
            }

            @Override
            public void clearContent()
            {

            }
        }, blockedSlot, xPosition, yPosition);
    }

    @Override
    public boolean mayPlace(ItemStack stack)
    {
        return ConfigData.isItemStackAllowed(stack);
    }
}
