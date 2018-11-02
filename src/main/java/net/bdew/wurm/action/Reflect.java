package net.bdew.wurm.action;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollInventory;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.client.renderer.gui.SelectBar;

import java.lang.reflect.Field;

@SuppressWarnings("unchecked")
public class Reflect {
    // HeadsUpDisplay
    static private Field fldPaperdollInventory;

    // PaperDollInventory
    static private Field fldBodyItem;

    // Active Tool
    static private Field fldActiveToolItem;

    // Selected Unit
    static private Field fldSelectedUnit;


    static public void setup() throws ReflectiveOperationException {
        fldPaperdollInventory = HeadsUpDisplay.class.getDeclaredField("paperdollInventory");
        fldPaperdollInventory.setAccessible(true);

        fldBodyItem = PaperDollInventory.class.getDeclaredField("bodyItem");
        fldBodyItem.setAccessible(true);

        fldActiveToolItem = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
        fldActiveToolItem.setAccessible(true);

        fldSelectedUnit = SelectBar.class.getDeclaredField("selectedUnit");
        fldSelectedUnit.setAccessible(true);
    }

    public static PaperDollInventory getPaperdollInventory(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return (PaperDollInventory) fldPaperdollInventory.get(hud);
    }

    public static InventoryMetaItem getBodyItem(PaperDollInventory pd) throws ReflectiveOperationException {
        return ((PaperDollSlot) fldBodyItem.get(pd)).getItem();
    }

    public static InventoryMetaItem getActiveToolItem(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return (InventoryMetaItem) fldActiveToolItem.get(hud);
    }

    public static PickableUnit getSelectedUnit(SelectBar s) throws ReflectiveOperationException {
        return (PickableUnit) fldSelectedUnit.get(s);
    }
}
