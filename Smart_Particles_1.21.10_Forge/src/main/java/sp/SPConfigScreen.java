package sp;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SPConfigScreen extends Screen {
    private final Screen parent;
    private EditBox limitBox;
    private boolean smartCulling;

    public SPConfigScreen(Screen parent) {
        super(Component.translatable("Smart Particles Config"));
        this.parent = parent;
        this.smartCulling = SPConfig.instance.smartCameraCulling;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Toggle Button for Smart Camera Culling
        // Explicitly typed to handle generic inference warnings
        CycleButton<Boolean> cullingBtn = CycleButton.onOffBuilder(this.smartCulling)
            .create(centerX - 100, startY, 200, 20, 
                Component.literal("Smart Camera Culling"), 
                (button, value) -> this.smartCulling = value);
        this.addRenderableWidget(cullingBtn);

        // Edit Box for Particle Limit
        this.limitBox = new EditBox(this.font, centerX - 100, startY + 30, 200, 20, Component.literal("Particle Limit"));
        this.limitBox.setValue(String.valueOf(SPConfig.instance.particleLimit));
        this.limitBox.setFilter(s -> s.matches("\\d*")); // Only allow numbers
        this.addRenderableWidget(this.limitBox);

        // Save & Exit Button
        Button saveBtn = Button.builder(Component.literal("Save & Exit"), b -> {
            save();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(centerX - 100, this.height - 40, 200, 20).build();
        this.addRenderableWidget(saveBtn);
    }

    private void save() {
        SPConfig.instance.smartCameraCulling = this.smartCulling;
        try {
            int limit = Integer.parseInt(this.limitBox.getValue());
            SPConfig.instance.particleLimit = Math.max(0, limit);
        } catch (NumberFormatException e) {
            // Keep old value if invalid
        }
        SPConfig.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Ensure limitBox is initialized before accessing
        if (this.limitBox != null) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Particle Limit:"), this.width / 2, this.limitBox.getY() - 12, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}