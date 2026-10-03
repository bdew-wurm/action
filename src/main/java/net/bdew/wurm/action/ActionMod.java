package net.bdew.wurm.action;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ActionMod implements WurmClientMod, Initable, PreInitable {
    private static final Logger logger = Logger.getLogger("ActionMod");

    public static boolean showActionNums = false;
    public static HeadsUpDisplay hud;

    public static void logException(String msg, Throwable e) {
        if (logger != null)
            logger.log(Level.SEVERE, msg, e);
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
                    "if (net.bdew.wurm.action.ActionHandler.handleInput($1,$2)) return true;"
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
