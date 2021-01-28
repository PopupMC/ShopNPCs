package com.popupmc.shopnpcs;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackagerMenu {
    public static void openMenu(Player p, String name) {
        // Create Merchant
        Merchant merchant = Bukkit.createMerchant(name);

        // Setup recipes
        List<MerchantRecipe> recipes = new ArrayList<>();
        addTrades(recipes);

        // apply recipes to merchant:
        merchant.setRecipes(recipes);

        // open trading window:
        p.openMerchant(merchant, true);
    }

    static void addTrades(List<MerchantRecipe> recipes) {
        disposableBox(recipes);
    }

    static void disposableBox(List<MerchantRecipe> recipes) {
        ItemStack udb = new ItemStack(Material.BLACK_SHULKER_BOX);

        BlockStateMeta udbMeta = (BlockStateMeta)  udb.getItemMeta();
        ShulkerBox udbBox = (ShulkerBox) udbMeta.getBlockState();

        List<String> lore = Collections.singletonList(ShopNpcs.udbLore);

        udbBox.setCustomName("Disposable Box");
        udbMeta.setDisplayName("Disposable Box");
        udbMeta.setLore(lore);
        udbMeta.setBlockState(udbBox);
        udb.setItemMeta(udbMeta);

        MerchantRecipe recipe = new MerchantRecipe(udb, 10000);
        recipe.addIngredient(new ItemStack(Material.CHEST));

        recipes.add(recipe);
    }
}
