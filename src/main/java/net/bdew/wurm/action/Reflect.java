package net.bdew.wurm.action;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollInventory;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.client.renderer.gui.SelectBar;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Reflect {
    // PaperDollInventory
    static private Field fldBodyItem;
    static private Method mGetFrameFromSlotnumber;

    // HeadsUpDisplay
    static private Field fldActiveToolItem;

    // SelectBar
    static private Field fldSelectedUnit;

    // ServerConnectionListenerClass
    private static Field fldGroundItems;

    static public void setup() throws ReflectiveOperationException {
        fldBodyItem = PaperDollInventory.class.getDeclaredField("bodyItem");
        fldActiveToolItem = HeadsUpDisplay.class.getDeclaredField("activeToolItem");
        fldSelectedUnit = SelectBar.class.getDeclaredField("selectedUnit");
        mGetFrameFromSlotnumber = ReflectionUtil.getMethod(PaperDollInventory.class, "getFrameFromSlotnumber", new Class[]{byte.class});
        fldGroundItems = ReflectionUtil.getField(ServerConnectionListenerClass.class, "groundItems");
    }

    public static InventoryMetaItem getBodyItem(PaperDollInventory pd) throws ReflectiveOperationException {
        return ((PaperDollSlot) ReflectionUtil.getPrivateField(pd, fldBodyItem)).getItem();
    }

    public static InventoryMetaItem getActiveToolItem(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, fldActiveToolItem);
    }

    public static PickableUnit getSelectedUnit(SelectBar s) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(s, fldSelectedUnit);
    }

    public static PaperDollSlot getFrameFromSlotnumber(PaperDollInventory pd, byte slot) throws ReflectiveOperationException {
        return ReflectionUtil.callPrivateMethod(pd, mGetFrameFromSlotnumber, slot);
    }

    public static Map<Long, GroundItemCellRenderable> getGroundItems(ServerConnectionListenerClass conn) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(conn, fldGroundItems);
    }
}
