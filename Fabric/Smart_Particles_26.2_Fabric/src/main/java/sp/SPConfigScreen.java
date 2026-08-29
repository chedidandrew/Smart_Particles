package sp;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.net.URI;

public final class SPConfigScreen extends Screen {
    private static final int CONTROL_WIDTH = 220;
    private static final int CONTROL_HEIGHT = 20;

    private final Screen parent;
    private EditBox limitField;
    private CycleButton<Boolean> cullingButton;
    private boolean smartCulling;

    public SPConfigScreen(Screen parent) {
        super(Component.literal("Smart Particles"));
        this.parent = parent;
        this.smartCulling = SPConfig.get().smartCameraCulling;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - CONTROL_WIDTH / 2;
        int y = this.height / 4 + 20;

        this.cullingButton = CycleButton.onOffBuilder(this.smartCulling)
                .create(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, Component.literal("Smart Camera Culling"),
                        (button, value) -> this.smartCulling = value);
        this.addRenderableWidget(this.cullingButton);

        this.limitField = new EditBox(this.font, x, y + 38, CONTROL_WIDTH, CONTROL_HEIGHT,
                Component.literal("Particle Limit"));
        this.limitField.setValue(Integer.toString(SPConfig.get().particleLimit));
        this.limitField.setMaxLength(7);
        this.addRenderableWidget(this.limitField);

        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), button -> {
            this.smartCulling = true;
            this.cullingButton.setValue(true);
            this.limitField.setValue(Integer.toString(SPConfig.DEFAULT_PARTICLE_LIMIT));
        }).bounds(x, y + 66, CONTROL_WIDTH, CONTROL_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.literal("Save & Close"), button -> {
            saveSettings();
            this.minecraft.gui.setScreen(this.parent);
        }).bounds(x, y + 90, CONTROL_WIDTH, CONTROL_HEIGHT).build());

        int linkWidth = 68;
        int spacing = 8;
        int linksWidth = linkWidth * 3 + spacing * 2;
        int startX = this.width / 2 - linksWidth / 2;
        int linkY = y + 122;

        this.addRenderableWidget(Button.builder(Component.literal("Ko-fi"),
                button -> openLink("https://ko-fi.com/andrewchedid"))
                .bounds(startX, linkY, linkWidth, CONTROL_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal("Modrinth"),
                button -> openLink("https://modrinth.com/mod/smart-particles"))
                .bounds(startX + linkWidth + spacing, linkY, linkWidth, CONTROL_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal("CurseForge"),
                button -> openLink("https://www.curseforge.com/minecraft/mc-mods/smart-particles"))
                .bounds(startX + (linkWidth + spacing) * 2, linkY, linkWidth, CONTROL_HEIGHT).build());
    }

    private void saveSettings() {
        SPConfig config = SPConfig.get();
        config.smartCameraCulling = this.smartCulling;

        int parsed = SPConfig.DEFAULT_PARTICLE_LIMIT;
        try {
            if (!this.limitField.getValue().isEmpty()) {
                parsed = Integer.parseInt(this.limitField.getValue());
            }
        } catch (NumberFormatException ignored) {
            // Keep the default value when the field is not a valid integer.
        }

        config.particleLimit = SPConfig.clampParticleLimit(parsed);
        SPConfig.save();
    }

    private void openLink(String url) {
        this.minecraft.gui.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(URI.create(url));
            }
            this.minecraft.gui.setScreen(this);
        }, url, true));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        int x = this.width / 2 - CONTROL_WIDTH / 2;
        int labelY = this.height / 4 + 46;
        graphics.text(this.font, Component.literal("Particle Limit"), x, labelY, 0xFFFFFFFF);
    }
}
