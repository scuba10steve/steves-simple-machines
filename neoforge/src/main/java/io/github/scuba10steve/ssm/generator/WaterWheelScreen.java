package io.github.scuba10steve.ssm.generator;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WaterWheelScreen extends AbstractContainerScreen<WaterWheelMenu> {

    // Water Wheel has no real slots (only player inventory) — unlike the shared
    // processing_machine.png, this texture has the vestigial input-slot/progress-arrow art
    // (baked in for the machine screens' use) painted over, since it would otherwise show as
    // two non-functional boxes with nothing behind them. Same energy-bar sprite region as
    // processing_machine.png (kept for CombustionGeneratorScreen's own art).
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/container/simple_generator.png");

    // Actual PNG dimensions — see ProcessingMachineScreen for why the 9-arg GuiGraphics#blit
    // overload (which takes these explicitly) is required instead of the 7-arg convenience
    // overload, which hardcodes 256x256 and silently corrupts the sampled region on any other size.
    private static final int TEXTURE_WIDTH = 200;
    private static final int TEXTURE_HEIGHT = 166;

    private static final int ENERGY_BAR_BG_U = 182;
    private static final int ENERGY_BAR_U = 176;
    private static final int ENERGY_BAR_V = 16;
    private static final int ENERGY_BAR_W = 6;
    private static final int ENERGY_BAR_H = 50;

    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 18;

    public WaterWheelScreen(WaterWheelMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0f, 0f, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

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

        // No slots to the left of the energy bar on this screen, so this space (freed by
        // painting over the vestigial slot/arrow art) is used for a stored/capacity/rate
        // readout instead of the title-row label the slotted screens use.
        int rate = menu.getCurrentRate();
        String rateText = rate > 0 ? "Rate: +" + rate + " FE/t" : "Rate: Idle";
        graphics.drawString(font, "Stored: " + menu.getEnergyStored() + " FE", 8, 20, 0x404040, false);
        graphics.drawString(font, "Capacity: " + menu.getEnergyCapacity() + " FE", 8, 32, 0x404040, false);
        graphics.drawString(font, rateText, 8, 44, 0x404040, false);
    }
}
