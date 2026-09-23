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

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        int progress = menu.getProgress();
        int total = menu.getProcessTicks();
        if (total > 0) {
            int filled = (progress * 24) / total;
            graphics.blit(TEXTURE, x + 79, y + 34, 176, 0, filled, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("container.inventory"), 8, imageHeight - 96, 0x404040, false);
    }
}
