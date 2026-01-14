package sp;

import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.net.URI;

public class SPConfigScreen extends Screen {
    private final Screen parent;
    private EditBox limitField;
    private CycleButton<Boolean> cullingButton;
    private boolean smartCulling;

    // Layout constants
    private static final int W = 200;
    private static final int H = 20;

    public SPConfigScreen(Screen parent) {
        super(Component.literal("Smart Particles"));
        this.parent = parent;
        this.smartCulling = SPConfig.instance.smartCameraCulling;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 4 + 24;

        // Smart Culling (Top)
        this.cullingButton = CycleButton.onOffBuilder(this.smartCulling)
                .create(x, y, W, H, Component.literal("Smart Culling"),
                        (button, value) -> this.smartCulling = value);
        this.addRenderableWidget(this.cullingButton);

        // Particle Limit (Second)
        this.limitField = new EditBox(this.font, x, y + 34, W, H, Component.literal("Particle Limit"));
        this.limitField.setValue(String.valueOf(SPConfig.instance.particleLimit));

        // Digits only (allow empty while editing)
        this.limitField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));

        // Optional: placeholder shown only when empty
        // this.limitField.setHint(Component.literal("5000"));

        this.addRenderableWidget(this.limitField);

        // Reset Button (Third)
        this.addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), b -> {
            this.smartCulling = true;
            this.cullingButton.setValue(true);
            this.limitField.setValue("5000");
        }).bounds(x, y + 58, W, H).build());

        // Save & Quit Button (Fourth)
        this.addRenderableWidget(Button.builder(Component.literal("Save & Quit"), b -> {
            save();
            this.minecraft.setScreen(this.parent);
        }).bounds(x, y + 82, W, H).build());

        // Links (Bottom)
        int buttonWidth = 60;
        int spacing = 10;
        int startX = this.width / 2 - (buttonWidth * 3 + spacing * 2) / 2;
        int yPos = y + 106;

        this.addRenderableWidget(Button.builder(Component.literal("Ko-Fi"), b -> openLink("https://ko-fi.com/andrewchedid"))
                .bounds(startX, yPos, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Modrinth"), b -> openLink("https://modrinth.com/mod/smart-particles"))
                .bounds(startX + buttonWidth + spacing, yPos, buttonWidth, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Curse"), b -> openLink("https://www.curseforge.com/minecraft/mc-mods/smart-particles"))
                .bounds(startX + (buttonWidth + spacing) * 2, yPos, buttonWidth, 20).build());
    }

    private void openLink(String url) {
        this.minecraft.setScreen(new ConfirmLinkScreen((yes) -> {
            if (yes) {
                Util.getPlatform().openUri(URI.create(url));
            }
            this.minecraft.setScreen(this);
        }, url, true));
    }

    private void save() {
        SPConfig.instance.smartCameraCulling = this.smartCulling;

        int parsed = 5000;
        try {
            String s = this.limitField.getValue();
            if (!s.isEmpty()) parsed = Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}

        if (parsed < 0) parsed = 0;
        SPConfig.instance.particleLimit = parsed;

        SPConfig.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Title (alpha included)
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);

        // Visible label for the EditBox (EditBox does not draw its message)
        int x = this.width / 2 - 100;
        int labelY = this.height / 4 + 24 + 34 - 12;
        guiGraphics.drawString(this.font, Component.literal("Particle Limit"), x, labelY, 0xFFFFFFFF);
    }
}
