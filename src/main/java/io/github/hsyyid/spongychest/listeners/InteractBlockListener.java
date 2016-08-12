package io.github.hsyyid.spongychest.listeners;

import com.google.inject.Inject;
import io.github.hsyyid.spongychest.SpongyChest;
import io.github.hsyyid.spongychest.data.isspongychest.IsSpongyChestData;
import io.github.hsyyid.spongychest.data.isspongychest.SpongeIsSpongyChestData;
import io.github.hsyyid.spongychest.data.itemchest.ItemChestData;
import io.github.hsyyid.spongychest.data.itemchest.SpongeItemChestData;
import io.github.hsyyid.spongychest.data.pricechest.PriceChestData;
import io.github.hsyyid.spongychest.data.pricechest.SpongePriceChestData;
import io.github.hsyyid.spongychest.data.uuidchest.SpongeUUIDChestData;
import io.github.hsyyid.spongychest.data.uuidchest.UUIDChestData;
import io.github.hsyyid.spongychest.utils.ChestShopModifier;
import io.github.hsyyid.spongychest.utils.ChestUtils;

import org.slf4j.Logger;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class InteractBlockListener
{
	@Inject
	private Logger logger;

	@Listener
	public void onPlayerInteractBlock(InteractBlockEvent.Secondary event, @Root Player player)
	{
		Supplier exception=new Supplier() {
			@Override
			public Object get() {
				return new IllegalStateException();
			}
		};
			if (event.getTargetBlock().getLocation().isPresent() && event.getTargetBlock().getState().getType() == BlockTypes.CHEST)
            {
				Chest chest= (Chest) event.getTargetBlock().getLocation().get().getTileEntity().get();

				if(checkChest(chest,player)){
					event.setCancelled(true);

				}
                else if (player.hasPermission("spongychest.shop.create"))
                {
					try {
						Optional<ChestShopModifier> chestShopModifier = SpongyChest.chestShopModifiers.stream().filter(m -> m.getUuid().equals(player.getUniqueId())).findAny();

						if (chestShopModifier.isPresent())
                        {
                            chest.offer(new SpongeIsSpongyChestData(true));
                            chest.offer(new SpongeItemChestData(chestShopModifier.get().getItem()));
                            chest.offer(new SpongePriceChestData(chestShopModifier.get().getPrice().doubleValue()));
                            chest.offer(new SpongeUUIDChestData(chestShopModifier.get().getUuid()));
                            SpongyChest.chestShopModifiers.remove(chestShopModifier.get());

                            Direction direction=chest.get(Keys.DIRECTION).orElseThrow((Supplier<RuntimeException>) () -> new IllegalStateException("Cannot determine direction of chest"));
                            Location<World> frameLocation = chest.getLocation().getBlockRelative(direction);

                            BlockState sign=BlockTypes.WALL_SIGN.getDefaultState().with(Keys.DIRECTION,direction).orElseThrow((Supplier<RuntimeException>)()->new IllegalStateException("Cannot set direction of sign"));
                            frameLocation.setBlock(sign,Cause.of(NamedCause.source(SpawnCause.builder().type(SpawnTypes.PLUGIN).build())));

    //                        ItemFrame itemFrame = (ItemFrame) chest.getLocation().getExtent().createEntity(EntityTypes.ITEM_FRAME, frameLocation.getPosition());
    //
    //                        if (itemFrame != null)
    //                        {
    //                            ItemStack frameStack = chestShopModifier.get().getItem().createStack();
    //                            frameStack.offer(Keys.DISPLAY_NAME, Text.of(TextColors.GREEN, "Item: ", TextColors.WHITE, frameStack.getTranslation().get(), " ", TextColors.GREEN, "Amount: ", TextColors.WHITE, frameStack.getQuantity(), " ", TextColors.GREEN, "Price: ", TextColors.WHITE, SpongyChest.economyService.getDefaultCurrency().getSymbol().toPlain(), chestShopModifier.get().getPrice()));
    //                            itemFrame.offer(Keys.REPRESENTED_ITEM, frameStack.createSnapshot());
    //                            ((EntityHanging) itemFrame).updateFacingWithBoundingBox(EnumFacing.byName(chest.getLocation().getBlock().get(Keys.DIRECTION).get().name()));
    //
    //                            if (((EntityHanging) itemFrame).onValidSurface())
    //                            {
    //                                chest.getLocation().getExtent().spawnEntity(itemFrame, Cause.of(NamedCause.source(SpawnCause.builder().type(SpawnTypes.PLUGIN).build())));
    //                            }
    //                        }

                            player.sendMessage(Text.of(TextColors.BLUE, "[SpongyChest]: ", TextColors.GREEN, "Created shop."));
                            event.setCancelled(true);
                        }
					} catch (RuntimeException e) {
						logger.error("Failed to setup shop: %s",e);
						player.sendMessage(Text.of(TextColors.BLUE, "[SpongyChest]: ", TextColors.GREEN, "Failed to setup shop."));
					}
				}
            }
			else if(event.getTargetBlock().getLocation().isPresent()&&event.getTargetBlock().getState().getType() == BlockTypes.WALL_SIGN){
				Location<World> location=event.getTargetBlock().getLocation().get();
				Direction dir=location.getBlock().get(Keys.DIRECTION).orElseThrow((Supplier<RuntimeException>) () -> new IllegalStateException("Cannot determine direction of sign"));
				Location<World> loc2=location.getBlockRelative(dir.getOpposite());
				if (loc2.getBlock().getType() == BlockTypes.CHEST){
					Chest chest= (Chest) loc2.getTileEntity().get();
					if(checkChest(chest,player)){
						event.setCancelled(true);
					}
				}
			}

	}

	/**
	 *
	 * @param chest
	 * @param player
     * @return If chest is spongy shop
     */
	private boolean checkChest(Chest chest,Player player) {

		if (chest.get(IsSpongyChestData.class).isPresent() && chest.get(IsSpongyChestData.class).get().isSpongyChest().get()) {
			ItemStackSnapshot item = chest.get(ItemChestData.class).get().itemStackSnapshot().get();
			double price = chest.get(PriceChestData.class).get().price().get();
			UUID ownerUuid = chest.get(UUIDChestData.class).get().uuid().get();

			if (player.getUniqueId().equals(ownerUuid)) {
				return true;
			}

			if (ChestUtils.containsItem(chest, item)) {
				UniqueAccount ownerAccount = SpongyChest.economyService.getOrCreateAccount(ownerUuid).get();
				UniqueAccount userAccount = SpongyChest.economyService.getOrCreateAccount(player.getUniqueId()).get();

				if (userAccount.transfer(ownerAccount, SpongyChest.economyService.getDefaultCurrency(), new BigDecimal(price), Cause.of(NamedCause.source(player))).getResult() == ResultType.SUCCESS) {
					ChestUtils.removeItems(chest, item);
					player.getInventory().offer(item.createStack());
					player.sendMessage(Text.of(TextColors.BLUE, "[SpongyChest]: ", TextColors.GREEN, "Purchased item(s)."));
				} else {
					player.sendMessage(Text.of(TextColors.BLUE, "[SpongyChest]: ", TextColors.RED, "You don't have enough money to use this shop."));
				}
			} else {
				player.sendMessage(Text.of(TextColors.BLUE, "[SpongyChest]: ", TextColors.RED, "This shop is out of stock."));
			}
			return true;
		}
		return false;
	}

}
