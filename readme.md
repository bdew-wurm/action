# Custom Actions mod for Wurm Unlimited (Client)

Requires [Ago's Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher/releases) to run.

This mod is free software: you can redistribute it and/or modify it under the terms of the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl-3.0.en.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

See the [forum thread](https://forum.wurmonline.com/index.php?/topic/136575-released-improved-compass-no-winter-better-tooltips-custom-actions-max-toolbelt-time-lock-skill-gain-tracker-updated-june-2/) for more details and my other mods.


#### Brief instructions
Type "act_show on" in the console. This will let you see action IDs on the right click menu.

![action ids](http://i.imgur.com/aY4voyx.jpg)

Bind your hotkey using 'bind <key> "act <id> hover"'. The double quotes around the action are required. For example bind ctrl+b "act 222 hover" will open the bank window if you hover your mouse over a settlement token and press ctrl+b

![binding](http://i.imgur.com/2WXkmXf.jpg)

#### All options

```
bind B "act <id> <target>"
bind B "act <id> <target> | <id> <target>"
bind B "act <id> <target> | <id> <target> | <id> <target>"
```
_Chaining commands will queue actions. You must have high enough ML for the number of actions_

| Target      |                                                                 |
| ----------- | --------------------------------------------------------------- |
| `hover`     | Uses the specified action upon the hovered item, object or tile |
| `body`      | Uses the specified action upon the characters body              |
| `tool`      | Uses the specified action upon the currently activated item     |
| `selected`  | Uses the specified action upon the selected tile/object         |
| `tile`      | Uses the specified action on current tile                       |
| `tile_{dir}`| Uses the specified action on nearby tile (n,e,w,s,ne,nw,se,sw)  |
| `area`      | Uses the specified action on 3x3 tiles around current tile      |
| `@tb{n}`    | Uses the specified action on item in toolbelt slot #n           |
| `toolbar`   | Activates the tool in toolbar slot `<id>`                       |

#### Examples

| Command                         | Description                                |
| ------------------------------- | ------------------------------------------ |
| `bind space "act 3 toolbelt \| 154 tile \| 4 toolbelt \| 318 tile"` | Activate slot 3 (say, a shovel) and pack the tile you are standing on, after that it will activate slot 4 (say a rake) and cultivate the same tile |
| `bind r "act 163 hand"` | Repair your current tool |
| `bind f "act 183 @tb1"` | Drinks from toolbelt slot #1 (if it contains water) |
