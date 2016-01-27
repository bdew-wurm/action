package net.bdew.wurm.action;

import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmMod;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionMod implements WurmMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("ActionMod");

    public static boolean showActionNums = true;
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
                    showActionNums = true;
                    return true;
                }
            }
            hud.consoleOutput("Usage: act_show {on|off}");
            return true;
        } else if (cmd.equals("act")) {
            if (data.length == 3) {
                short id;
                try {
                    id = Short.parseShort(data[1]);
                } catch (Exception e) {
                    hud.consoleOutput("act: Error parsing id");
                    return true;
                }
                if (data[2].equals("hover")) {
                    hud.getWorld().sendHoveredAction(new PlayerAction(id, PlayerAction.ANYTHING));
                    return true;
                } else if (data[2].equals("body")) {
                    try {
                        hud.sendAction(new PlayerAction(id, PlayerAction.ANYTHING), Reflect.getBodyItem(Reflect.getPaperdollInventory(hud)).getId());
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    return true;
                } else if (data[2].equals("tile")) {
                    hud.getWorld().sendLocalAction(new PlayerAction(id, PlayerAction.ANYTHING));
                    return true;
                }
            }
            hud.consoleOutput("Usage: act <id> {hover|body|tile}");
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
}
