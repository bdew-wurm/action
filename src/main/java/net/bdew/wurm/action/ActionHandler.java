package net.bdew.wurm.action;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.shared.constants.PlayerAction;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ActionHandler {
    public static boolean handleInput(final String cmd, final String[] data) {
        if (cmd.equals("act_show")) {
            if (data.length == 2) {
                if (data[1].equals("on")) {
                    ActionMod.hud.consoleOutput("Action numbers on");
                    ActionMod.showActionNums = true;
                    return true;
                } else if (data[1].equals("off")) {
                    ActionMod.hud.consoleOutput("Action numbers off");
                    ActionMod.showActionNums = false;
                    return true;
                }
            }
            ActionMod.hud.consoleOutput("Usage: act_show {on|off}");
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
                        ActionMod.hud.consoleOutput("Usage: act <id> <modifier>[|<id> <modifier>|...]");
                } catch (ReflectiveOperationException roe) {
                    throw new RuntimeException(roe);
                } catch (NumberFormatException nfe) {
                    ActionMod.hud.consoleOutput("act: Error parsing id '" + nextCmdSplit[0] + "'");
                }
            }
            return true;
        }
        return false;
    }

    private static void sendAreaAction(final PlayerAction action, final Predicate<Long> filter) {
        sendLocalAction(action, +1, +1, filter);
        sendLocalAction(action, +1, +0, filter);
        sendLocalAction(action, +1, -1, filter);

        sendLocalAction(action, +0, +1, filter);
        sendLocalAction(action, +0, +0, filter);
        sendLocalAction(action, +0, -1, filter);

        sendLocalAction(action, -1, +1, filter);
        sendLocalAction(action, -1, +0, filter);
        sendLocalAction(action, -1, -1, filter);
    }

    private static void sendLocalAction(final PlayerAction action, int xo, int yo, final Predicate<Long> filter) {
        int x = ActionMod.hud.getWorld().getPlayerCurrentTileX();
        int y = ActionMod.hud.getWorld().getPlayerCurrentTileY();
        long tid = Tiles.getTileId(x + xo, y + yo, 0);
        if (filter == null || filter.test(tid)) {
            ActionMod.hud.sendAction(action, tid);
        }
    }

    private static boolean isTreeTile(final long tileId) {
        int x = Tiles.decodeTileX(tileId);
        int y = Tiles.decodeTileY(tileId);
        Tiles.Tile tileType = ActionMod.hud.getWorld().getNearTerrainBuffer().getTileType(x, y);
        return tileType.isTree() || tileType.isBush();
    }

    private static void parseAct(final short id, final String target) throws ReflectiveOperationException {
        PlayerAction act = new PlayerAction(id, PlayerAction.ANYTHING, "", false);
        switch (target) {
            case "hover":
                ActionMod.hud.getWorld().sendHoveredAction(act);
                break;
            case "body":
                ActionMod.hud.sendAction(act, Reflect.getBodyItem(ActionMod.hud.getPaperDollInventory()).getId());
                break;
            case "tile":
                ActionMod.hud.getWorld().sendLocalAction(act);
                break;
            case "tile_n":
                sendLocalAction(act, 0, -1, null);
                break;
            case "tile_w":
                sendLocalAction(act, -1, 0, null);
                break;
            case "tile_nw":
                sendLocalAction(act, -1, -1, null);
                break;
            case "tile_ne":
                sendLocalAction(act, 1, -1, null);
                break;
            case "tile_s":
                sendLocalAction(act, 0, 1, null);
                break;
            case "tile_e":
                sendLocalAction(act, 1, 0, null);
                break;
            case "tile_se":
                sendLocalAction(act, 1, 1, null);
                break;
            case "tile_sw":
                sendLocalAction(act, -1, 1, null);
                break;
            case "tool":
                InventoryMetaItem t = Reflect.getActiveToolItem(ActionMod.hud);
                if (t != null)
                    ActionMod.hud.sendAction(act, t.getId());
                else
                    ActionMod.hud.consoleOutput("act: tool modifier requires an active tool selected");
                break;
            case "selected":
                PickableUnit p = Reflect.getSelectedUnit(ActionMod.hud.getSelectBar());
                if (p != null)
                    ActionMod.hud.sendAction(act, p.getId());
                break;
            case "area":
                sendAreaAction(act, null);
                break;
            case "trees":
                sendAreaAction(act, ActionHandler::isTreeTile);
                break;
            case "toolbelt":
                if (id >= 1 && id <= 10)
                    ActionMod.hud.setActiveTool(id - 1);
                else
                    ActionMod.hud.consoleOutput("act: Invalid toolbelt slot '" + id + "'");
                break;
            default:
                if (target.startsWith("@tb")) {
                    int slot = Integer.parseInt(target.substring(3));
                    if (slot >= 1 && slot <= 10 && ActionMod.hud.getToolBelt().getItemInSlot(slot - 1) != null)
                        ActionMod.hud.sendAction(act, ActionMod.hud.getToolBelt().getItemInSlot(slot - 1).getId());
                    else
                        ActionMod.hud.consoleOutput("act: Invalid toolbelt slot '" + slot + "'");
                } else if (target.startsWith("@eq")) {
                    byte slot = Byte.parseByte(target.substring(3));
                    PaperDollSlot obj = Reflect.getFrameFromSlotnumber(ActionMod.hud.getPaperDollInventory(), slot);
                    if (obj == null) {
                        ActionMod.hud.consoleOutput("act: Invalid equipment slot " + slot);
                    } else if (obj.getEquippedItem() == null) {
                        ActionMod.hud.consoleOutput("act: No item in equipment slot " + slot);
                    } else {
                        ActionMod.hud.sendAction(act, obj.getEquippedItem().getId());
                    }
                } else if (target.startsWith("@nearby")) {
                    float range = Float.parseFloat(target.substring(7));
                    final float rangeSq = range * range;
                    ServerConnectionListenerClass conn = ActionMod.hud.getWorld().getServerConnection().getServerConnectionListener();
                    Collection<GroundItemCellRenderable> items = Reflect.getGroundItems(conn).values();
                    Collection<CreatureCellRenderable> creatures = conn.getCreatures().values();
                    Stream.concat(items.stream(), creatures.stream())
                            .filter(x -> x.getSquaredLengthFromPlayer() < rangeSq)
                            .mapToLong(CellRenderable::getId)
                            .forEach(tid -> ActionMod.hud.sendAction(act, tid));
                } else {
                    ActionMod.hud.consoleOutput("act: Invalid target keyword '" + target + "'");
                }
        }
    }
}
