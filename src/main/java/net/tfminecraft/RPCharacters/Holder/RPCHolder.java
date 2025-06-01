package net.tfminecraft.RPCharacters.Holder;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import net.tfminecraft.RPCharacters.Creation.Stage;

public class RPCHolder implements InventoryHolder {
    private final Player owner;
    private Stage stage;
    private boolean overridden = false;

    public RPCHolder(Player p) {
        this.owner = p;
        this.stage = null;
    }
    public RPCHolder(Player p, Stage stage) {
        this.owner = p;
        this.stage = stage;
    }

    public Player getOwner() {
        return owner;
    }

    public Stage getStage() {
        return stage;
    }

    public void override() {
        overridden = true;
    }

    public boolean isOverridden() {
        return overridden;
    }

    @Override
    public Inventory getInventory() {
        return null; // Not used in this case
    }
}
