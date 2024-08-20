package net.bdew.wurm.action;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.shared.constants.PlayerAction;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookException;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ActionMod implements WurmClientMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("ActionMod");

    public static boolean showActionNums = false;
    public static HeadsUpDisplay hud;

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
    }

    public static boolean handleInput(final String cmd, final String[] data) {
        if (cmd.equals("act_show")) {
            if (data.length == 2) {
                if (data[1].equals("on")) {
                    hud.consoleOutput("Action numbers on");
                    showActionNums = true;
                    return true;
                } else if (data[1].equals("off")) {
                    hud.consoleOutput("Action numbers off");
                    showActionNums = false;
                    return true;
                }
            }
            hud.consoleOutput("Usage: act_show {on|off}");
            return true;
        } else if (cmd.equals("act")) {
            // Stitch it back together with spaces, without the leading 'act' and get a list of strings split by |
            final String[] commands = String.join(" ", Arrays.copyOfRange(data, 1, data.length)).split("\\|");
            for (String nextCmd : commands) {
                // Remove leading/trailing whitespace, then split it apart and parse it
                final String[] nextCmdSplit = nextCmd.trim().split(" ");
                try {
                    if (nextCmdSplit.length == 2)
                        parseAct(Short.parseShort(nextCmdSplit[0]), nextCmdSplit[1]);
                    else
                        hud.consoleOutput("Usage: act <id> <modifier>[|<id> <modifier>|...]");
                } catch (ReflectiveOperationException roe) {
                    throw new RuntimeException(roe);
                } catch (NumberFormatException nfe) {
                    hud.consoleOutput("act: Error parsing id '" + nextCmdSplit[0] + "'");
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        logger.fine("Initializing");
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();

            CtClass ctPlayerAction = classPool.getCtClass("com.wurmonline.shared.constants.PlayerAction");
            ctPlayerAction.getMethod("getName", "()Ljava/lang/String;")
                    .insertBefore("if (net.bdew.wurm.action.ActionMod.showActionNums) return this.name + \" (\"+this.id+\")\";");

            CtClass ctWurmConsole = classPool.getCtClass("com.wurmonline.client.console.WurmConsole");
            ctWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore(
                    "if (net.bdew.wurm.action.ActionMod.handleInput($1,$2)) return true;"
            );

            // Hook HUD init to setup our stuff
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                    "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);
                hud = (HeadsUpDisplay) proxy;
                Reflect.setup();
                return null;
            });

            //Enable quick keybinds for non DEV players
            try {
                classPool.get("com.wurmonline.client.renderer.gui.WurmPopup").getDeclaredMethod("startQuickKeybind")
                        .instrument(getExprEditorForDEVOverwrite());
            } catch (NotFoundException e) {
                throw new HookException("Method startQuickKeybind not found in WurmPopup.class. " + e);
            }

            //Enable quick mousebinds for non DEV players
            try {
                classPool.get("com.wurmonline.client.renderer.gui.WurmPopup").getDeclaredMethod("startQuickMousebind")
                        .instrument(getExprEditorForDEVOverwrite());
            } catch (NotFoundException e) {
                throw new HookException("Method startQuickMousebind not found in WurmPopup.class. " + e);
            }

            //Overwrite some DEV checks to enable binding of all actions
            try {
                classPool.get("com.wurmonline.client.console.WurmConsole").getDeclaredMethod("toggleKey")
                        .instrument(getExprEditorForDEVOverwrite());
            } catch (NotFoundException e) {
                throw new HookException("Method toggleKey not found in WurmConsole.class. " + e);
            }

            try {
                classPool.get("com.wurmonline.client.renderer.gui.WurmPopup").getDeclaredMethod("rebindPrimary")
                        .instrument(getExprEditorForDEVOverwrite());
            } catch (NotFoundException e) {
                throw new HookException("Method rebindPrimary not found in WurmPopup.class. " + e);
            }

            try {
                classPool.get("com.wurmonline.client.renderer.gui.WurmPopup").getDeclaredMethod("updateWithKeybinds")
                        .instrument(getExprEditorForDEVOverwrite());
            } catch (NotFoundException e) {
                throw new HookException("Method updateWithKeybinds not found in WurmPopup.class. " + e);
            }

            logger.fine("Loaded");
        } catch (Throwable e) {
            logException("Error loading mod", e);
        }
    }

    @Override
    public void preInit() {

    }

    private static void sendAreaAction(final PlayerAction action) {
        sendLocalAction(action, +1, +1);
        sendLocalAction(action, +1, +0);
        sendLocalAction(action, +1, -1);

        sendLocalAction(action, +0, +1);
        sendLocalAction(action, +0, +0);
        sendLocalAction(action, +0, -1);

        sendLocalAction(action, -1, +1);
        sendLocalAction(action, -1, +0);
        sendLocalAction(action, -1, -1);
    }

    private static void sendLocalAction(final PlayerAction action, int xo, int yo) {
        int x = hud.getWorld().getPlayerCurrentTileX();
        int y = hud.getWorld().getPlayerCurrentTileY();
        hud.sendAction(action, Tiles.getTileId(x + xo, y + yo, 0));
    }

    private static void parseAct(final short id, final String target) throws ReflectiveOperationException {
        PlayerAction act = new PlayerAction(id, PlayerAction.ANYTHING, "", false);
        switch (target) {
            case "hover":
                hud.getWorld().sendHoveredAction(act);
                break;
            case "body":
                hud.sendAction(act, Reflect.getBodyItem(hud.getPaperDollInventory()).getId());
                break;
            case "tile":
                hud.getWorld().sendLocalAction(act);
                break;
            case "tile_n":
                sendLocalAction(act, 0, -1);
                break;
            case "tile_w":
                sendLocalAction(act, -1, 0);
                break;
            case "tile_nw":
                sendLocalAction(act, -1, -1);
                break;
            case "tile_ne":
                sendLocalAction(act, 1, -1);
                break;
            case "tile_s":
                sendLocalAction(act, 0, 1);
                break;
            case "tile_e":
                sendLocalAction(act, 1, 0);
                break;
            case "tile_se":
                sendLocalAction(act, 1, 1);
                break;
            case "tile_sw":
                sendLocalAction(act, -1, 1);
                break;
            case "tool":
                InventoryMetaItem t = Reflect.getActiveToolItem(hud);
                if (t != null)
                    hud.sendAction(act, t.getId());
                else
                    hud.consoleOutput("act: tool modifier requires an active tool selected");
                break;
            case "selected":
                PickableUnit p = Reflect.getSelectedUnit(hud.getSelectBar());
                if (p != null)
                    hud.sendAction(act, p.getId());
                break;
            case "area":
                sendAreaAction(act);
                break;
            case "toolbelt":
                if (id >= 1 && id <= 10)
                    hud.setActiveTool(id - 1);
                else
                    hud.consoleOutput("act: Invalid toolbelt slot '" + id + "'");
                break;
            default:
                if (target.startsWith("@tb")) {
                    int slot = Integer.parseInt(target.substring(3));
                    if (slot >= 1 && slot <= 10 && hud.getToolBelt().getItemInSlot(slot - 1) != null)
                        hud.sendAction(act, hud.getToolBelt().getItemInSlot(slot - 1).getId());
                    else
                        hud.consoleOutput("act: Invalid toolbelt slot '" + slot + "'");
                } else if (target.startsWith("@eq")) {
                    byte slot = Byte.parseByte(target.substring(3));
                    PaperDollSlot obj = Reflect.getFrameFromSlotnumber(hud.getPaperDollInventory(), slot);
                    if (obj == null) {
                        hud.consoleOutput("act: Invalid equipment slot " + slot);
                    } else if (obj.getEquippedItem() == null) {
                        hud.consoleOutput("act: No item in equipment slot " + slot);
                    } else {
                        hud.sendAction(act, obj.getEquippedItem().getId());
                    }
                } else if (target.startsWith("@nearby")) {
                    float range = Float.parseFloat(target.substring(7));
                    final float rangeSq = range * range;
                    ServerConnectionListenerClass conn = hud.getWorld().getServerConnection().getServerConnectionListener();
                    Collection<GroundItemCellRenderable> items = Reflect.getGroundItems(conn).values();
                    Collection<CreatureCellRenderable> creatures = conn.getCreatures().values();
                    Stream.concat(items.stream(), creatures.stream())
                            .filter(x -> x.getSquaredLengthFromPlayer() < rangeSq)
                            .mapToLong(CellRenderable::getId)
                            .forEach(tid -> hud.sendAction(act, tid));
                } else {
                    hud.consoleOutput("act: Invalid target keyword '" + target + "'");
                }
        }
    }

    private static ExprEditor getExprEditorForDEVOverwrite() {
        return new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                if ("com.wurmonline.client.comm.SimpleServerConnectionClass".equals(m.getClassName())
                        && m.getMethodName().equals("isDev")) {
                    String replaceString = "{ $_ = true; }";
                    m.replace(replaceString);
                }
            }
        };
    }
}
