package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ProcessingMachineScreen extends AbstractContainerScreen<ProcessingMachineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/container/processing_machine.png");

    public ProcessingMachineScreen(ProcessingMachineMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    // Actual PNG dimensions (176px GUI + a 24px progress-arrow strip to the right = 200 wide, 166 tall).
    // The 7-arg GuiGraphics#blit(ResourceLocation, x, y, u, v, width, height) convenience overload
    // hardcodes textureWidth/textureHeight to 256x256 for UV normalization (confirmed via javap on
    // the real class) — since this texture isn't 256x256, that overload scales the sampled region
    // incorrectly. The 9-arg overload takes the real texture dimensions explicitly instead.
    private static final int TEXTURE_WIDTH = 200;
    private static final int TEXTURE_HEIGHT = 166;

    // Must match ProcessingMachineMenu's own output slot layout (outputStartX = 116, one slot
    // every 18px, at y = 35) — there's no shared accessor for this, matching how the progress-bar
    // position above is also a hardcoded literal kept in sync with the menu by convention.
    private static final int OUTPUT_SLOT_START_X = 116;
    private static final int OUTPUT_SLOT_Y = 35;

    // Output slot count varies per machine (Smelter uses 1; Crusher and Sawmill use 2 — but this
    // must not assume any fixed count). Rather than baking a fixed number of slot outlines into the
    // background PNG (which only lines up for whichever machine happens to have that many real
    // slots — this was a real bug: the texture used to draw 3 output boxes, matching none of the
    // machines' actual output counts, leaving 1-2 non-functional "ghost" boxes per machine), the
    // input slot's own 18x18 box art is reused as a stamp and blitted once per real output slot.
    private static final int SLOT_ART_U = 55;
    private static final int SLOT_ART_V = 34;
    private static final int SLOT_ART_SIZE = 18;

    // Same energy-bar convention already used by all 4 generator screens (identical U/V/W/H/X/Y),
    // so every FE-consuming or FE-producing block in the mod shows its buffer level consistently.
    private static final int ENERGY_BAR_BG_U = 182;
    private static final int ENERGY_BAR_U = 176;
    private static final int ENERGY_BAR_V = 16;
    private static final int ENERGY_BAR_W = 6;
    private static final int ENERGY_BAR_H = 50;
    private static final int ENERGY_BAR_X = 152;
    private static final int ENERGY_BAR_Y = 18;

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0f, 0f, imageWidth, imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        for (int i = 0; i < menu.getOutputSlotCount(); i++) {
            int slotX = x + OUTPUT_SLOT_START_X + (i * 18) - 1;
            int slotY = y + OUTPUT_SLOT_Y - 1;
            graphics.blit(TEXTURE, slotX, slotY, 0, (float) SLOT_ART_U, (float) SLOT_ART_V,
                    SLOT_ART_SIZE, SLOT_ART_SIZE, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        int progress = menu.getProgress();
        int total = menu.getProcessTicks();
        if (total > 0) {
            int filled = (progress * 24) / total;
            graphics.blit(TEXTURE, x + 79, y + 34, 0, 176f, 0f, filled, 16, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        }

        // Background is drawn full-height every time, regardless of charge level, so the bar's
        // presence is visible even at 0 energy — previously, only the fill was drawn, so an empty
        // buffer rendered nothing at all, indistinguishable from having no indicator.
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

        // Title row is the only free horizontal strip across the 176x166 layout — the energy
        // bar fills nearly the entire vertical band between the title and the inventory label,
        // leaving no room above/below it for text (see ENERGY_BAR_Y/H above).
        String energyText = menu.getEnergyStored() + " FE";
        graphics.drawString(font, energyText, imageWidth - 8 - font.width(energyText), 6, 0x404040, false);
    }
}
