package net.bdew.wurm.action;

import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
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
            for(String nextCmd : commands) {
                // Remove leading/trailing whitespace, then split it apart and parse it
                final String[] nextCmdSplit = nextCmd.trim().split(" ");
                try { 
                    if(nextCmdSplit.length == 2)
                        parseAct(Short.parseShort(nextCmdSplit[0]),nextCmdSplit[1]);
                    else
                        hud.consoleOutput("Usage: act <id> {hover|body|tile|selected}[|<id> {...}|...]");
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
    
    private static void parseAct(final short id, final String target) throws ReflectiveOperationException {
        switch(target) {
            case "hover":
                hud.getWorld().sendHoveredAction(new PlayerAction(id, PlayerAction.ANYTHING));
                break;
            case "body":
                hud.sendAction(new PlayerAction(id, PlayerAction.ANYTHING), Reflect.getBodyItem(Reflect.getPaperdollInventory(hud)).getId());
                break;
            case "tile":
                hud.getWorld().sendLocalAction(new PlayerAction(id, PlayerAction.ANYTHING));
                break;
            case "selected":
                PickableUnit p = Reflect.getSelectedUnit(hud.getSelectBar());
                if(p != null)
                    hud.sendAction(new PlayerAction(id, PlayerAction.ANYTHING), p.getId());
                break;
            default:
                hud.consoleOutput("act: Invalid target keyword '" + target + "'");
        }        
    }    
}
