package sp;

import net.minecraft.Util;
import com.mojang.blaze3d.vertex.PoseStack;
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

    public SPConfigScreen(Screen parent) {
        super(Component.literal("Smart Particles"));
        this.parent = parent;
        this.smartCulling = SPConfig.instance.smartCameraCulling;
    }

    @Override
    protected void init() {
        super.init();

        // 1. Smart Culling Toggle (1.19.2: booleanBuilder instead of onOffBuilder)
        this.cullingButton = CycleButton.booleanBuilder(
                        Component.literal("ON"),
                        Component.literal("OFF")
                )
                .withInitialValue(this.smartCulling)
                .create(
                        this.width / 2 - 100,
                        this.height / 4 + 24,
                        200,
                        20,
                        Component.literal("Smart Culling"),
                        (button, value) -> this.smartCulling = value
                );
        this.addRenderableWidget(this.cullingButton);

        // 2. Particle Limit Text Field
        this.limitField = new EditBox(
                this.font,
                this.width / 2 - 100,
                this.height / 4 + 48 + 20,
                200,
                20,
                Component.literal("Particle Limit")
        );
        this.limitField.setValue(String.valueOf(SPConfig.instance.particleLimit));
        this.limitField.setFilter(text -> text.matches("\\d*"));
        this.addRenderableWidget(this.limitField);

        // 3. Reset Button
        this.addRenderableWidget(new Button(
                this.width / 2 - 100,
                this.height - 88,
                200,
                20,
                Component.literal("Reset to Defaults"),
                button -> {
                    this.smartCulling = true;
                    if (this.cullingButton != null) this.cullingButton.setValue(true);
                    if (this.limitField != null) this.limitField.setValue("5000");
                }
        ));

        // 4. Save & Quit Button
        this.addRenderableWidget(new Button(
                this.width / 2 - 100,
                this.height - 64,
                200,
                20,
                Component.literal("Save & Quit"),
                button -> {
                    save();
                    this.minecraft.setScreen(this.parent);
                }
        ));

        // 5. Link Buttons (Ko-fi | Modrinth | Curseforge)
        int buttonWidth = 64;
        int spacing = 4;
        int startX = this.width / 2 - ((buttonWidth * 3) + (spacing * 2)) / 2;
        int yPos = this.height - 30;

        this.addRenderableWidget(new Button(
                startX,
                yPos,
                buttonWidth,
                20,
                Component.literal("Ko-fi"),
                b -> openLink("https://ko-fi.com/andrewchedid")
        ));

        this.addRenderableWidget(new Button(
                startX + buttonWidth + spacing,
                yPos,
                buttonWidth,
                20,
                Component.literal("Modrinth"),
                b -> openLink("https://modrinth.com/mod/smart-particles")
        ));

        this.addRenderableWidget(new Button(
                startX + (buttonWidth + spacing) * 2,
                yPos,
                buttonWidth,
                20,
                Component.literal("Curse"),
                b -> openLink("https://www.curseforge.com/minecraft/mc-mods/smart-particles")
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.limitField != null) {
            this.limitField.tick();
        }
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
        try {
            SPConfig.instance.particleLimit = Integer.parseInt(this.limitField.getValue());
        } catch (NumberFormatException e) {
            SPConfig.instance.particleLimit = 5000;
        }
        SPConfig.save();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);

        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawCenteredString(
                poseStack,
                this.font,
                Component.literal("Particle Limit:"),
                this.width / 2,
                this.height / 4 + 48 + 5,
                0xA0A0A0
        );
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}