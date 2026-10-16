package com.elyrecode.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

@Environment(EnvType.CLIENT)
public class ElyrecodeClient implements ClientModInitializer {
    public static ElyrecodeConfig config;
    public static LivingEntity lastAttackedEntity = null;
    public static String lastAttackedName = "";

    // Armor slots top to bottom: helmet, chestplate, leggings, boots
    public static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    @Override
    public void onInitializeClient() {
        config = ElyrecodeConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            com.mojang.brigadier.Command<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> openCmd = context -> {
                MinecraftClient.getInstance().send(() ->
                    MinecraftClient.getInstance().setScreen(new ElyrecodeConfigScreen()));
                return 1;
            };
            dispatcher.register(ClientCommandManager.literal("elyrecode").executes(openCmd));
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof LivingEntity living) {
                lastAttackedEntity = living;
                lastAttackedName = living instanceof PlayerEntity p
                    ? p.getName().getString()
                    : living.getName().getString();
            }
            return ActionResult.PASS;
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden
                    || client.currentScreen instanceof ElyrecodeConfigScreen) {
                return;
            }

            boolean hasOpponent = lastAttackedEntity != null && lastAttackedEntity.isAlive() 
                && lastAttackedEntity.squaredDistanceTo(client.player) < 1024;

            renderArmorItems(drawContext, client.player, false, hasOpponent ? lastAttackedEntity : null);

            if (hasOpponent) {
                renderArmorItems(drawContext, lastAttackedEntity, true, client.player);

                if (config.showOpponentName && lastAttackedName != null && !lastAttackedName.isEmpty()) {
                    TextRenderer tr = client.textRenderer;
                    int textWidth = tr.getWidth(lastAttackedName);
                    int nameX;
                    int nameY;

                    switch (config.nameMode) {
                        case ABOVE_ARMOR -> {
                            nameX = config.opponentX + 8 - textWidth / 2;
                            nameY = config.opponentY - 11;
                        }
                        case CROSSHAIR -> {
                            int screenW = client.getWindow().getScaledWidth();
                            int screenH = client.getWindow().getScaledHeight();
                            nameX = (screenW - textWidth) / 2;
                            nameY = (screenH / 2) + config.crosshairYOffset;
                        }
                        case FREE -> {
                            nameX = config.opponentNameX;
                            nameY = config.opponentNameY;
                        }
                        default -> {
                            nameX = config.opponentNameX;
                            nameY = config.opponentNameY;
                        }
                    }

                    drawContext.drawTextWithShadow(tr, Text.literal(lastAttackedName), nameX, nameY, 0xFFFFFFFF);
                }
            }
        });
    }

    private void renderArmorItems(DrawContext ctx, LivingEntity entity, boolean isOpponent, LivingEntity compareTo) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        
        int x = isOpponent ? config.opponentX : config.playerX;
        int y = isOpponent ? config.opponentY : config.playerY;

        int textOffsetX = 18;
        int textOffsetY = 4;
        
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = entity.getEquippedStack(slot);
            int rowY = y + (i * 18);

            int color = 0xFFFFFFFF;

            if (compareTo != null) {
                ItemStack compareStack = compareTo.getEquippedStack(slot);
                int myCur = stack != null && stack.isDamageable() ? (stack.getMaxDamage() - stack.getDamage()) : 0;
                int oppCur = compareStack != null && compareStack.isDamageable() ? (compareStack.getMaxDamage() - compareStack.getDamage()) : 0;
                
                // Color comparison independent per piece
                if (isOpponent) {
                    if (myCur > oppCur) color = 0xFF55FF55;
                    else if (myCur < oppCur) color = 0xFFFF5555;
                } else {
                    if (myCur > oppCur) color = 0xFF55FF55;
                    else if (myCur < oppCur) color = 0xFFFF5555;
                }
            }

            if (stack != null && !stack.isEmpty()) {
                ctx.drawItem(stack, x, rowY);
                renderDurabilityBar(ctx, tr, stack, x, rowY);

                if (stack.isDamageable()) {
                    String val = config.showPercentage
                        ? ((int)(((double)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage()) * 100)) + "%"
                        : String.valueOf(stack.getMaxDamage() - stack.getDamage());
                    ctx.drawTextWithShadow(tr, Text.literal(val), x + textOffsetX, rowY + textOffsetY, color);
                }
            } else {
                ctx.drawTextWithShadow(tr, Text.literal("-"), x + textOffsetX, rowY + textOffsetY, 0xFF555555);
            }
        }
    }

    public static void renderDurabilityBar(DrawContext ctx, TextRenderer tr, ItemStack stack, int x, int y) {
        if (!config.showDurabilityBar || stack == null || stack.isEmpty() || !stack.isDamageable()) {
            return;
        }

        if (stack.isItemBarVisible()) {
            ctx.drawStackOverlay(tr, stack, x, y);
        } else if (config.showBarWhenFull && stack.getDamage() == 0) {
            int barX = x + 2;
            int barY = y + 13;
            ctx.fill(barX, barY, barX + 13, barY + 2, 0xFF000000);
            ctx.fill(barX, barY, barX + 13, barY + 1, 0xFF00FF00);
        }
    }
}
