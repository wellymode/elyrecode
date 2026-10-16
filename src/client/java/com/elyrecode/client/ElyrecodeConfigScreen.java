package com.elyrecode.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ElyrecodeConfigScreen extends Screen {
    private boolean draggingPlayer = false;
    private boolean draggingOpponent = false;
    private boolean draggingOpponentName = false;
    private boolean wasMouseDown = false;

    private int startMouseX = 0;
    private int startMouseY = 0;
    private int origPlayerX = 0;
    private int origPlayerY = 0;
    private int origOpponentX = 0;
    private int origOpponentY = 0;
    private int origOpponentNameX = 0;
    private int origOpponentNameY = 0;
    private int origCrosshairYOffset = 0;

    // Visual guide lines when magnetic snapping is triggered
    private Integer guideLineX = null;
    private Integer guideLineY = null;

    private static final int SNAP_DIST = 5;
    private static final int[] GRID_OPTIONS = {0, 1, 2, 4, 8, 16, 32};

    private static final ItemStack[] DUMMY;
    static {
        DUMMY = new ItemStack[] {
            new ItemStack(Items.IRON_HELMET),
            new ItemStack(Items.IRON_CHESTPLATE),
            new ItemStack(Items.IRON_LEGGINGS),
            new ItemStack(Items.IRON_BOOTS)
        };
        DUMMY[0].setDamage(25);  // ~85% (green)
        DUMMY[1].setDamage(96);  // ~60% (yellow)
        DUMMY[2].setDamage(155); // ~31% (orange-red)
        DUMMY[3].setDamage(175); // ~10% (red)
    }

    protected ElyrecodeConfigScreen() {
        super(Text.literal("elyrecode config"));
    }

    @Override
    protected void init() {
        super.init();
        int cx = this.width / 2;
        int btnW = 110;
        int btnH = 20;

        int row1Y = this.height - 100;
        int row2Y = this.height - 76;
        int row3Y = this.height - 52;
        int row4Y = this.height - 28;

        // Row 1: Mode (% / #) and Opponent Name (ON / OFF)
        addDrawableChild(ButtonWidget.builder(
            Text.literal(modeLabel()),
            btn -> {
                ElyrecodeClient.config.showPercentage = !ElyrecodeClient.config.showPercentage;
                btn.setMessage(Text.literal(modeLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx - 114, row1Y, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(opponentNameLabel()),
            btn -> {
                ElyrecodeClient.config.showOpponentName = !ElyrecodeClient.config.showOpponentName;
                btn.setMessage(Text.literal(opponentNameLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx + 4, row1Y, btnW, btnH).build());

        // Row 2: Name Position Mode and Smart Snapping
        addDrawableChild(ButtonWidget.builder(
            Text.literal(nameModeLabel()),
            btn -> {
                ElyrecodeConfig.NameMode[] modes = ElyrecodeConfig.NameMode.values();
                int nextIdx = (ElyrecodeClient.config.nameMode.ordinal() + 1) % modes.length;
                ElyrecodeClient.config.nameMode = modes[nextIdx];
                btn.setMessage(Text.literal(nameModeLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx - 114, row2Y, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(snappingLabel()),
            btn -> {
                ElyrecodeClient.config.smartSnapping = !ElyrecodeClient.config.smartSnapping;
                btn.setMessage(Text.literal(snappingLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx + 4, row2Y, btnW, btnH).build());

        // Row 3: Durability Bars and Grid
        addDrawableChild(ButtonWidget.builder(
            Text.literal(barsLabel()),
            btn -> {
                if (ElyrecodeClient.config.showDurabilityBar && !ElyrecodeClient.config.showBarWhenFull) {
                    ElyrecodeClient.config.showBarWhenFull = true;
                } else if (ElyrecodeClient.config.showDurabilityBar && ElyrecodeClient.config.showBarWhenFull) {
                    ElyrecodeClient.config.showDurabilityBar = false;
                    ElyrecodeClient.config.showBarWhenFull = false;
                } else {
                    ElyrecodeClient.config.showDurabilityBar = true;
                    ElyrecodeClient.config.showBarWhenFull = false;
                }
                btn.setMessage(Text.literal(barsLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx - 114, row3Y, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal(gridLabel()),
            btn -> {
                int cur = ElyrecodeClient.config.gridSize;
                int idx = 0;
                for (int i = 0; i < GRID_OPTIONS.length; i++) {
                    if (GRID_OPTIONS[i] == cur) { idx = i; break; }
                }
                ElyrecodeClient.config.gridSize = GRID_OPTIONS[(idx + 1) % GRID_OPTIONS.length];
                btn.setMessage(Text.literal(gridLabel()));
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx + 4, row3Y, btnW, btnH).build());

        // Row 4: Reset Pos and Done
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Reset Pos"),
            btn -> {
                ElyrecodeClient.config.playerX = 10;
                ElyrecodeClient.config.playerY = 30;
                ElyrecodeClient.config.opponentX = 80;
                ElyrecodeClient.config.opponentY = 30;
                ElyrecodeClient.config.opponentNameX = 80;
                ElyrecodeClient.config.opponentNameY = 16;
                ElyrecodeClient.config.crosshairYOffset = 18;
                ElyrecodeClient.config.save();
            }
        ).dimensions(cx - 114, row4Y, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(
            Text.literal("Done"),
            btn -> this.close()
        ).dimensions(cx + 4, row4Y, btnW, btnH).build());
    }

    private String modeLabel() {
        return ElyrecodeClient.config.showPercentage ? "Mode: %" : "Mode: #";
    }

    private String opponentNameLabel() {
        return ElyrecodeClient.config.showOpponentName ? "Opp Name: ON" : "Opp Name: OFF";
    }

    private String nameModeLabel() {
        return "Name: " + ElyrecodeClient.config.nameMode.label;
    }

    private String snappingLabel() {
        return ElyrecodeClient.config.smartSnapping ? "Snap: ON" : "Snap: OFF";
    }

    private String barsLabel() {
        if (!ElyrecodeClient.config.showDurabilityBar) {
            return "Bars: OFF";
        }
        return ElyrecodeClient.config.showBarWhenFull ? "Bars: Always" : "Bars: Vanilla";
    }

    private String gridLabel() {
        int g = ElyrecodeClient.config.gridSize;
        return g == 0 ? "Grid: Off" : "Grid: " + g + "px";
    }

    private int snap(int v) {
        int g = ElyrecodeClient.config.gridSize;
        return g <= 0 ? v : Math.round((float) v / g) * g;
    }

    private int blockW() { return 52; }
    private int blockH() { return 18 * 4; }

    private int nameBoxW() { return 65; }
    private int nameBoxH() { return 14; }

    private int getEffectiveNameX() {
        return switch (ElyrecodeClient.config.nameMode) {
            case ABOVE_ARMOR -> ElyrecodeClient.config.opponentX + 8 - nameBoxW() / 2;
            case CROSSHAIR -> (this.width - nameBoxW()) / 2;
            case FREE -> ElyrecodeClient.config.opponentNameX;
        };
    }

    private int getEffectiveNameY() {
        return switch (ElyrecodeClient.config.nameMode) {
            case ABOVE_ARMOR -> ElyrecodeClient.config.opponentY - 16;
            case CROSSHAIR -> (this.height / 2) + ElyrecodeClient.config.crosshairYOffset;
            case FREE -> ElyrecodeClient.config.opponentNameY;
        };
    }

    private boolean inBlock(int mx, int my, int bx, int by) {
        return mx >= bx - 2 && mx <= bx + blockW() + 2
            && my >= by - 2 && my <= by + blockH() + 2;
    }

    private boolean inNameBlock(int mx, int my, int bx, int by) {
        return mx >= bx - 2 && mx <= bx + nameBoxW() + 2
            && my >= by - 2 && my <= by + nameBoxH() + 2;
    }

    private static record Box(int x, int y, int w, int h) {}

    private int snapX(int targetX, int w, List<Box> others) {
        guideLineX = null;
        if (!ElyrecodeClient.config.smartSnapping) {
            return snap(targetX);
        }

        int screenCenterX = this.width / 2;
        int elemCenterX = targetX + w / 2;

        // 1. Center of screen
        if (Math.abs(elemCenterX - screenCenterX) <= SNAP_DIST) {
            guideLineX = screenCenterX;
            return screenCenterX - w / 2;
        }

        // 2. Relative to other elements
        for (Box b : others) {
            // Left to Left
            if (Math.abs(targetX - b.x) <= SNAP_DIST) {
                guideLineX = b.x;
                return b.x;
            }
            // Center to Center
            int otherCenterX = b.x + b.w / 2;
            if (Math.abs(elemCenterX - otherCenterX) <= SNAP_DIST) {
                guideLineX = otherCenterX;
                return otherCenterX - w / 2;
            }
            // Right to Right
            if (Math.abs((targetX + w) - (b.x + b.w)) <= SNAP_DIST) {
                guideLineX = b.x + b.w;
                return b.x + b.w - w;
            }
        }

        return snap(targetX);
    }

    private int snapY(int targetY, int h, List<Box> others) {
        guideLineY = null;
        if (!ElyrecodeClient.config.smartSnapping) {
            return snap(targetY);
        }

        int screenCenterY = this.height / 2;
        int elemCenterY = targetY + h / 2;

        // 1. Center of screen
        if (Math.abs(elemCenterY - screenCenterY) <= SNAP_DIST) {
            guideLineY = screenCenterY;
            return screenCenterY - h / 2;
        }

        // 2. Relative to other elements
        for (Box b : others) {
            // Top to Top
            if (Math.abs(targetY - b.y) <= SNAP_DIST) {
                guideLineY = b.y;
                return b.y;
            }
            // Center to Center
            int otherCenterY = b.y + b.h / 2;
            if (Math.abs(elemCenterY - otherCenterY) <= SNAP_DIST) {
                guideLineY = otherCenterY;
                return otherCenterY - h / 2;
            }
            // Bottom to Bottom
            if (Math.abs((targetY + h) - (b.y + b.h)) <= SNAP_DIST) {
                guideLineY = b.y + b.h;
                return b.y + b.h - h;
            }
        }

        return snap(targetY);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Render crosshair guide in CROSSHAIR mode
        if (ElyrecodeClient.config.nameMode == ElyrecodeConfig.NameMode.CROSSHAIR) {
            int cx = this.width / 2;
            int cy = this.height / 2;
            ctx.fill(cx - 5, cy, cx + 6, cy + 1, 0x99FFFFFF);
            ctx.fill(cx, cy - 5, cx + 1, cy + 6, 0x99FFFFFF);
            ctx.fill(cx, cy + 6, cx + 1, cy + ElyrecodeClient.config.crosshairYOffset, 0x4455FFFF);
        }

        // Draw magnetic alignment guide lines (Lunar Client style)
        if (guideLineX != null && ElyrecodeClient.config.smartSnapping) {
            ctx.fill(guideLineX, 0, guideLineX + 1, this.height, 0xAA00FFFF);
        }
        if (guideLineY != null && ElyrecodeClient.config.smartSnapping) {
            ctx.fill(0, guideLineY, this.width, guideLineY + 1, 0xAA00FFFF);
        }

        drawPreviewBlock(ctx, ElyrecodeClient.config.playerX, ElyrecodeClient.config.playerY, "You", draggingPlayer);
        drawPreviewBlock(ctx, ElyrecodeClient.config.opponentX, ElyrecodeClient.config.opponentY, "Opponent", draggingOpponent);
        drawOpponentNameBlock(ctx, getEffectiveNameX(), getEffectiveNameY(), draggingOpponentName);

        long win = MinecraftClient.getInstance().getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;

        // Prevent dragging when clicking buttons at bottom
        boolean inButtonArea = mouseY >= this.height - 105;

        int curNameX = getEffectiveNameX();
        int curNameY = getEffectiveNameY();

        if (down && !wasMouseDown && !inButtonArea) {
            if (inNameBlock(mouseX, mouseY, curNameX, curNameY)) {
                if (ElyrecodeClient.config.nameMode == ElyrecodeConfig.NameMode.ABOVE_ARMOR) {
                    // In Above Armor mode, dragging name moves the opponent armor block together
                    draggingOpponent = true;
                    startMouseX = mouseX;
                    startMouseY = mouseY;
                    origOpponentX = ElyrecodeClient.config.opponentX;
                    origOpponentY = ElyrecodeClient.config.opponentY;
                } else {
                    draggingOpponentName = true;
                    startMouseX = mouseX;
                    startMouseY = mouseY;
                    origOpponentNameX = ElyrecodeClient.config.opponentNameX;
                    origOpponentNameY = ElyrecodeClient.config.opponentNameY;
                    origCrosshairYOffset = ElyrecodeClient.config.crosshairYOffset;
                }
            } else if (inBlock(mouseX, mouseY, ElyrecodeClient.config.playerX, ElyrecodeClient.config.playerY)) {
                draggingPlayer = true;
                startMouseX = mouseX;
                startMouseY = mouseY;
                origPlayerX = ElyrecodeClient.config.playerX;
                origPlayerY = ElyrecodeClient.config.playerY;
            } else if (inBlock(mouseX, mouseY, ElyrecodeClient.config.opponentX, ElyrecodeClient.config.opponentY)) {
                draggingOpponent = true;
                startMouseX = mouseX;
                startMouseY = mouseY;
                origOpponentX = ElyrecodeClient.config.opponentX;
                origOpponentY = ElyrecodeClient.config.opponentY;
            }
        } else if (!down && wasMouseDown) {
            draggingPlayer = false;
            draggingOpponent = false;
            draggingOpponentName = false;
            guideLineX = null;
            guideLineY = null;
            ElyrecodeClient.config.save();
        }

        if (down) {
            int deltaX = mouseX - startMouseX;
            int deltaY = mouseY - startMouseY;

            if (draggingPlayer) {
                List<Box> others = List.of(
                    new Box(ElyrecodeClient.config.opponentX, ElyrecodeClient.config.opponentY, blockW(), blockH()),
                    new Box(curNameX, curNameY, nameBoxW(), nameBoxH())
                );
                int rawX = Math.max(0, Math.min(this.width - blockW(), origPlayerX + deltaX));
                int rawY = Math.max(0, Math.min(this.height - blockH(), origPlayerY + deltaY));
                ElyrecodeClient.config.playerX = snapX(rawX, blockW(), others);
                ElyrecodeClient.config.playerY = snapY(rawY, blockH(), others);
            } else if (draggingOpponent) {
                List<Box> others = List.of(
                    new Box(ElyrecodeClient.config.playerX, ElyrecodeClient.config.playerY, blockW(), blockH()),
                    new Box(curNameX, curNameY, nameBoxW(), nameBoxH())
                );
                int rawX = Math.max(0, Math.min(this.width - blockW(), origOpponentX + deltaX));
                int rawY = Math.max(0, Math.min(this.height - blockH(), origOpponentY + deltaY));
                ElyrecodeClient.config.opponentX = snapX(rawX, blockW(), others);
                ElyrecodeClient.config.opponentY = snapY(rawY, blockH(), others);
            } else if (draggingOpponentName) {
                if (ElyrecodeClient.config.nameMode == ElyrecodeConfig.NameMode.CROSSHAIR) {
                    guideLineX = this.width / 2; // Locked under crosshair
                    List<Box> others = List.of(
                        new Box(ElyrecodeClient.config.playerX, ElyrecodeClient.config.playerY, blockW(), blockH()),
                        new Box(ElyrecodeClient.config.opponentX, ElyrecodeClient.config.opponentY, blockW(), blockH())
                    );
                    int targetCenterY = (this.height / 2) + origCrosshairYOffset + deltaY;
                    int snappedY = snapY(targetCenterY, nameBoxH(), others);
                    ElyrecodeClient.config.crosshairYOffset = snappedY - (this.height / 2);
                } else if (ElyrecodeClient.config.nameMode == ElyrecodeConfig.NameMode.FREE) {
                    List<Box> others = List.of(
                        new Box(ElyrecodeClient.config.playerX, ElyrecodeClient.config.playerY, blockW(), blockH()),
                        new Box(ElyrecodeClient.config.opponentX, ElyrecodeClient.config.opponentY, blockW(), blockH())
                    );
                    int rawX = Math.max(0, Math.min(this.width - nameBoxW(), origOpponentNameX + deltaX));
                    int rawY = Math.max(0, Math.min(this.height - nameBoxH(), origOpponentNameY + deltaY));
                    ElyrecodeClient.config.opponentNameX = snapX(rawX, nameBoxW(), others);
                    ElyrecodeClient.config.opponentNameY = snapY(rawY, nameBoxH(), others);
                }
            }
        }
        wasMouseDown = down;
    }

    private void drawOpponentNameBlock(DrawContext ctx, int x, int y, boolean dragging) {
        int bw = nameBoxW();
        int bh = nameBoxH();
        int border = dragging ? 0xFFFFFF55 : (ElyrecodeClient.config.showOpponentName ? 0xFF888888 : 0xFF555555);

        ctx.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, dragging ? 0x99FFFFFF : 0x77000000);
        ctx.fill(x - 2, y - 2, x + bw + 2, y - 1,      border);
        ctx.fill(x - 2, y + bh + 1, x + bw + 2, y + bh + 2, border);
        ctx.fill(x - 2, y - 2, x - 1,      y + bh + 2, border);
        ctx.fill(x + bw + 1, y - 2, x + bw + 2, y + bh + 2, border);

        String txt;
        if (!ElyrecodeClient.config.showOpponentName) {
            txt = "§8Opponent (OFF)";
        } else {
            txt = switch (ElyrecodeClient.config.nameMode) {
                case ABOVE_ARMOR -> "§cOpp (Above)";
                case CROSSHAIR -> "§cOpp (Aim)";
                case FREE -> "§cOpponent";
            };
        }
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(txt), x + 4, y + 3, 0xFFFFFFFF);
    }

    private void drawPreviewBlock(DrawContext ctx, int x, int y, String label, boolean dragging) {
        int bw = blockW();
        int bh = blockH();
        int border = dragging ? 0xFFFFFF55 : 0xFF888888;

        ctx.fill(x - 2, y - 2, x + bw + 2, y + bh + 2, dragging ? 0x99FFFFFF : 0x77000000);
        ctx.fill(x - 2, y - 2, x + bw + 2, y - 1,      border);
        ctx.fill(x - 2, y + bh + 1, x + bw + 2, y + bh + 2, border);
        ctx.fill(x - 2, y - 2, x - 1,      y + bh + 2, border);
        ctx.fill(x + bw + 1, y - 2, x + bw + 2, y + bh + 2, border);

        ctx.drawTextWithShadow(this.textRenderer, Text.literal("§7[" + label + "]"), x, y - 11, 0xFFAAAAAA);

        for (int i = 0; i < DUMMY.length; i++) {
            int rowY = y + (i * 18);
            ctx.drawItem(DUMMY[i], x, rowY);
            ElyrecodeClient.renderDurabilityBar(ctx, this.textRenderer, DUMMY[i], x, rowY);

            int maxD = DUMMY[i].getMaxDamage();
            int curD = maxD - DUMMY[i].getDamage();
            String val = ElyrecodeClient.config.showPercentage
                ? ((int)(((double)curD / maxD) * 100)) + "%"
                : String.valueOf(curD);
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(val), x + 18, rowY + 4, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        ElyrecodeClient.config.save();
        super.close();
    }
}
