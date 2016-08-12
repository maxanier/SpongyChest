package io.github.hsyyid.spongychest.utils;

import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;

import java.util.Optional;

public class ChestUtils
{

	public static boolean containsItem(TileEntityCarrier entity,ItemStackSnapshot snapshot){

		int foundItems=0;
		Inventory inventory=entity.getInventory();
		Inventory slots=inventory.query(snapshot.getType());
		for(Inventory slot:slots){
			Optional<ItemStack> stack=slot.peek();
			if(stack.isPresent()){
				foundItems+=stack.get().getQuantity();
				if(foundItems>=snapshot.getCount()){
					return true;
				}
			}
		}
		return false;
	}


	public static void removeItems(TileEntityCarrier chest,ItemStackSnapshot snapshot){
		Inventory inventory=chest.getInventory();
		Inventory slots=inventory.query(snapshot.getType());
		int items=snapshot.getCount();
		while(items>0){
			Optional<ItemStack> result= slots.poll(items%snapshot.getType().getMaxStackQuantity());
			items-=snapshot.getType().getMaxStackQuantity();
		}
	}


}
