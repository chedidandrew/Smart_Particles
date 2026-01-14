package sp;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class SPConfigScreen extends Screen {
    private final Screen parent;

    private EditBox particleLimitField;
    private Checkbox smartCameraCullingBox;

    public SPConfigScreen(Screen parent) {
        super(Component.literal("Smart Particles Config"));
        this.parent = parent;
    }

    @Override
    @SuppressWarnings({ "null" })
    protected void init() {
        final int centerX = this.width / 2;
        final int y0 = this.height / 4;

        SPConfig cfg = SPConfig.get();

        this.particleLimitField = new EditBox(
            this.font,
            centerX - 100, y0,
            200, 20,
            Component.literal("Particle Limit")
        );
        this.particleLimitField.setValue(Integer.toString(cfg.particleLimit));
        this.particleLimitField.setMaxLength(10);
        this.addRenderableWidget(this.particleLimitField);

        this.smartCameraCullingBox = Checkbox.builder(Component.literal("Smart Camera Culling"), this.font)
            .pos(centerX - 100, y0 + 30)
            .selected(cfg.smartCameraCulling)
            .build();
        this.addRenderableWidget(this.smartCameraCullingBox);

        this.addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            applyAndSave();
            var mc = this.minecraft;
            if (mc != null) mc.setScreen(parent);
        }).pos(centerX - 100, y0 + 70).size(98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            var mc = this.minecraft;
            if (mc != null) mc.setScreen(parent);
        }).pos(centerX + 2, y0 + 70).size(98, 20).build());
    }

    private void applyAndSave() {
        SPConfig cfg = SPConfig.get();

        int limit = cfg.particleLimit;
        try {
            limit = Integer.parseInt(this.particleLimitField.getValue().trim());
        } catch (Exception ignored) {
        }

        if (limit < 0) limit = 0;

        cfg.particleLimit = limit;
        cfg.smartCameraCulling = this.smartCameraCullingBox.selected();

        SPConfig.save();
    }

    @Override
    public void onClose() {
        var mc = this.minecraft;
        if (mc != null) mc.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
