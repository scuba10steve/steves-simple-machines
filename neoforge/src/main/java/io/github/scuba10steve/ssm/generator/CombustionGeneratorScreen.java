package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CombustionGeneratorScreen extends AbstractContainerScreen<CombustionGeneratorMenu> {

    // Reuses the shared placeholder GUI texture from Task 6 (no dedicated art asset for v1);
    // the sprite region to the right of the 176x166 visible panel has enough room for both
    // the flame and energy-bar swatches used below.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/container/processing_machine.png");

    // Actual PNG dimensions — see ProcessingMachineScreen for why the 9-arg GuiGraphics#blit
    // overload (which takes these explicitly) is required instead of the 7-arg convenience
    // overload, which hardcodes 256x256 and silently corrupts the sampled region on any other size.
    private static final int TEXTURE_WIDTH = 200;
    private static final int TEXTURE_HEIGHT = 166;

    private static final int FLAME_U = 176;
    private static final int FLAME_V = 0;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 13;

    private static final int ENERGY_BAR_BG_U = 182;
    private static final int ENERGY_BAR_U = 176;
    private static final int ENERGY_BAR_V = 16;
    private static final int ENERGY_BAR_W = 6;
    private static final int ENERGY_BAR_H = 50;

    private static final int FLAME_X = 60;
    private static final int FLAME_Y = 43;
    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 18;

    public CombustionGeneratorScreen(CombustionGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0f, 0f, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        if (menu.isLit()) {
            int progress = menu.getBurnProgress();
            graphics.blit(TEXTURE, x + FLAME_X, y + FLAME_Y + (FLAME_H - progress), 0,
                    (float) FLAME_U, (float) (FLAME_V + (FLAME_H - progress)),
                    FLAME_W, progress, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Background drawn full-height every time so the bar is visible even at 0 energy.
        graphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y, 0, (float) ENERGY_BAR_BG_U, (float) ENERGY_BAR_V,
                ENERGY_BAR_W, ENERGY_BAR_H, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int capacity = menu.getEnergyCapacity();
        int filledHeight = capacity > 0 ? ENERGY_BAR_H * menu.getEnergyStored() / capacity : 0;
        graphics.blit(TEXTURE, x + ENERGY_BAR_X, y + ENERGY_BAR_Y + (ENERGY_BAR_H - filledHeight), 0,
                (float) ENERGY_BAR_U, (float) (ENERGY_BAR_V + (ENERGY_BAR_H - filledHeight)),
                ENERGY_BAR_W, filledHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, imageHeight - 96, 0x404040, false);
    }
}
