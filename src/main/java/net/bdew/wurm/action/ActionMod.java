package net.bdew.wurm.action;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.shared.constants.PlayerAction;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionMod implements WurmMod, Initable, PreInitable {
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
                        hud.consoleOutput("Usage: act <id> {hover|body|tile|selected|area}[|<id> {...}|...]");
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
            ctPlayerAction.getMethod("getName", "()Ljava/lang/String;").insertBefore("if (net.bdew.wurm.action.ActionMod.showActionNums) return this.name + \" (\"+this.id+\")\";");

            CtClass ctWurmConsole = classPool.getCtClass("com.wurmonline.client.console.WurmConsole");
            ctWurmConsole.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z").insertBefore(
                    "if (net.bdew.wurm.action.ActionMod.handleInput($1,$2)) return true;"
            );

            // Hook HUD init to setup our stuff
            HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
                method.invoke(proxy, args);
                hud = (HeadsUpDisplay) proxy;
                Reflect.setup();
                return null;
            });

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
        PlayerAction act = new PlayerAction(id, PlayerAction.ANYTHING);
        switch (target) {
            case "hover":
                hud.getWorld().sendHoveredAction(act);
                break;
            case "body":
                hud.sendAction(act, Reflect.getBodyItem(Reflect.getPaperdollInventory(hud)).getId());
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
                    if (slot >= 1 && slot <= 10)
                        hud.sendAction(act, hud.getToolBelt().getItemInSlot(slot - 1).getId());
                    else
                        hud.consoleOutput("act: Invalid toolbelt slot '" + id + "'");
                } else {
                    hud.consoleOutput("act: Invalid target keyword '" + target + "'");
                }
        }
    }
}
