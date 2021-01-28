package com.popupmc.shopnpcs;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

public class Packager extends Trait {
    public Packager() {
        super("packager");
    }

    // Handle NPC Right Clicks
    @EventHandler
    public void click(NPCRightClickEvent event){
        // Make sure it's this NPC
        if(event.getNPC() != this.getNPC())
            return;

        // Get Player
        Player p = event.getClicker();

        // Open Menu
        PackagerMenu.openMenu(p, this.getNPC().getName());
    }

    // Called every tick
    @Override
    public void run() {
    }

    @Override
    public void onAttach() {

    }

    @Override
    public void onDespawn() {
    }

    @Override
    public void onSpawn() {

    }

    @Override
    public void onRemove() {
    }

    @EventHandler
    public void onUDBPlace(BlockPlaceEvent e) {
        ItemStack udb = e.getItemInHand();

        if(udb.getType() != Material.BLACK_SHULKER_BOX)
            return;

        List<String> lore = e.getItemInHand().getLore();

        if(lore == null)
            return;

        if(lore.size() <= 0)
            return;

        // UDB Block Place
        if(lore.get(0).equals(ShopNpcs.udbLore))
            handleUDBPlace(e, udb, lore);

        // DB Block Place
        else if(lore.get(0).equals(ShopNpcs.dbLore))
            handleDBPlace(e, udb, lore);
    }

    public void handleUDBPlace(BlockPlaceEvent e, ItemStack udb, List<String> lore) {
        BlockStateMeta udbMeta = (BlockStateMeta) udb.getItemMeta();

        Inventory udp = Bukkit.createInventory(null, InventoryType.SHULKER_BOX, udbMeta.getDisplayName());
        e.getPlayer().openInventory(udp);

        e.setCancelled(true);
    }

    public void handleDBPlace(BlockPlaceEvent e, ItemStack db, List<String> lore) {
        // If it's just 1 line then let the other plugin handle it
        if(lore.size() == 1)
            return;

        // New lore to place
        ArrayList<String> newLore = new ArrayList<>();

        // Keep lookout for errors in parsing, go through the lore lines
        try {
            for (String line : lore) {

                // A cost lore means we need to charge the player
                if (line.startsWith(ShopNpcs.costLore)) {

                    // Get segs, must be 3
                    // Cost:0.04:junebug12851
                    String[] costSegs = line.split(":");
                    if(costSegs.length < 3) {
                        e.getPlayer().sendMessage(new String[]{
                                ChatColor.translateAlternateColorCodes('&',
                                        "&cThe Item cost is formatted wrong in the lore. Ignoring cost.")
                        });
                        continue;
                    }

                    // Get charge amount
                    float price = Float.parseFloat(costSegs[1]);

                    // Get to player
                    String destPlayer = costSegs[2];

                    // Charge player, if charge fails stop here, otherwise remove cost line from lore
                    if(!handleCost(e, db, lore, price, destPlayer)) {
                        e.setCancelled(true);
                        return;
                    }
                    else
                        continue;
                }

                // Add lines to lore
                newLore.add(line);
            }
        }
        // Upon number mis-format proceed on with error msg
        catch (NumberFormatException ex) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cThe Item cost is formatted wrong in the lore. Ignoring cost.")
            });
        }
        catch (Exception ex) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cAn internal error has occured. Proceeding anyways...")
            });
        }

        // Open inv from box
        BlockStateMeta boxMeta = (BlockStateMeta) e.getItemInHand().getItemMeta();
        ShulkerBox box = (ShulkerBox) boxMeta.getBlockState();
        Inventory boxInv = box.getInventory();

        // Set New Lore
        boxMeta.setLore(newLore);

        // Get size of box
        int maxCount = boxInv.getSize();

        // Whether the copy was successful or not
        boolean enoughSpace = true;

        // Copy items over from box
        for(int i = 0; i < maxCount; i++) {
            ItemStack item = boxInv.getItem(i);
            if(item == null)
                continue;

            // Stop here if no free space for item
            int freeSpace = invFreeSpace(e.getPlayer().getInventory(), item.getType(), item);
            if(freeSpace < item.getAmount()) {
                enoughSpace = false;
                break;
            }

            // Copy over and remove from box
            e.getPlayer().getInventory().addItem(item);
            boxInv.remove(item);
        }

        // Apply changes
        boxMeta.setBlockState(box);
        e.getItemInHand().setItemMeta(boxMeta);

        // Announce success or failure
        if(!enoughSpace) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cWarning: Not all items could be moved to your inventory. &6Please free up some space and try again.")
            });
        }
        else {
            // Also remove item now that its empty
            e.getPlayer().getInventory().remove(e.getItemInHand());

            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&aAll items have been placed in your inventory!")
            });
        }

        // Mark canceled
        e.setCancelled(true);
    }

    public boolean handleCost(BlockPlaceEvent e, ItemStack db, List<String> lore, float cost, String toPlayerName) {
        // Get current balance
        float curBal = (float)ShopNpcs.econ.getBalance(e.getPlayer());

        if(curBal < cost) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cYou don't have enough money to open this box. &6Need ❇" + cost + ".")
            });
            return false;
        }

        EconomyResponse r = ShopNpcs.econ.withdrawPlayer(e.getPlayer(), cost);

        if(!r.transactionSuccess()) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cA vault plugin error has occured, proceeding without cost.")
            });
            return true;
        }

        e.getPlayer().sendMessage(new String[]{
                ChatColor.translateAlternateColorCodes('&',
                        "&aYou were charged ❇" + cost)
        });

        OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(toPlayerName);

        // Add money to players inventory
        r = ShopNpcs.econ.depositPlayer(toPlayer, cost);

        if(!r.transactionSuccess()) {
            e.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&cVault error occured when paying " + toPlayerName + ", proceeding without paying player.")
            });
            return true;
        }

        e.getPlayer().sendMessage(new String[]{
                ChatColor.translateAlternateColorCodes('&',
                        "&a❇" + cost + " has been transferred successfully to " + toPlayerName)
        });

        if(toPlayer.isOnline() && toPlayer.getPlayer() != null) {
            toPlayer.getPlayer().sendMessage(new String[]{
                    ChatColor.translateAlternateColorCodes('&',
                            "&aYou've received ❇" + cost + " from " + e.getPlayer().getDisplayName() + " for a box of items you were charging for.")
            });
        }

        return true;
    }

    @EventHandler
    public void onUDBClose(InventoryCloseEvent e) {

        // Get Item in Hand
        ItemStack udb = e.getPlayer().getInventory().getItemInMainHand();

        // Make sure its a black shulker
        if(udb.getType() != Material.BLACK_SHULKER_BOX)
            return;

        // Get Shulker Meta
        BlockStateMeta udbMeta = (BlockStateMeta)  udb.getItemMeta();

        // Make sure the inventory name matches item in hand
        if(!e.getView().getTitle().equals(udbMeta.getDisplayName()))
            return;

        // Get Lore
        List<String> lore = udbMeta.getLore();

        // Make sure it exists aand theres at least 1 entry that matches "Unopened Disposable Box"
        if(lore == null)
            return;

        if(lore.size() < 1)
            return;

        if(!lore.get(0).equals(ShopNpcs.udbLore))
            return;

        // Breakdown title into pieces
        String[] titlePieces = udbMeta.getDisplayName().split(" ");
        ArrayList<String> newLore = new ArrayList<>();

        // Rebuild title and extract lore lines from it
        StringBuilder newTitle = new StringBuilder();
        for(String titlePiece : titlePieces) {

            // If it doesnt start with @ then its part of the title
            if(!titlePiece.startsWith("@")) {
                newTitle.append(titlePiece).append(" ");
                continue;
            }

            // If it starts with an @, remove the @ and add it as lore on its own line
            // Replace _ with a space
            titlePiece = titlePiece.substring(1);
            titlePiece = titlePiece.replace("_", " ");

            // Add new lore line with color code support
            newLore.add(ChatColor.translateAlternateColorCodes('&',titlePiece));
        }

        // Set new title with color code support
        udbMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', newTitle.toString()));

        // Get Open Inventory
        Inventory inv = e.getInventory();

        // Get Shulker Box Inventory
        ShulkerBox udbBox = (ShulkerBox) udbMeta.getBlockState();
        Inventory udbInv = udbBox.getInventory();

        // Copy contents over
        for(int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);

            if(item == null)
                continue;

            udbInv.setItem(i, item.clone());
        }

        // Set new lore
        ArrayList<String> dbLore = new ArrayList<>();
        dbLore.add(ShopNpcs.dbLore);
        dbLore.addAll(newLore);

        // Apply changes
        udbMeta.setBlockState(udbBox);
        udb.setItemMeta(udbMeta);
        udb.setLore(dbLore);
    }

    public int invFreeSpace(PlayerInventory inv, Material m, ItemStack item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot ++) {
            ItemStack is = inv.getItem(slot);
            if (is == null) {
                count += m.getMaxStackSize();
            }
            if (is != null) {
                if (is.isSimilar(item)){
                    count += (m.getMaxStackSize() - is.getAmount());
                }
            }
        }
        return count;
    }

    public static void invRemoveSpace(PlayerInventory inventory, ItemStack item, int amount) {
        if (amount <= 0) return;
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            ItemStack is = inventory.getItem(slot);
            if (is == null) continue;
            if (is.isSimilar(item)) {
                int newAmount = is.getAmount() - amount;
                if (newAmount > 0) {
                    is.setAmount(newAmount);
                    break;
                } else {
                    inventory.clear(slot);
                    amount = -newAmount;
                    if (amount == 0) break;
                }
            }
        }
    }
}
