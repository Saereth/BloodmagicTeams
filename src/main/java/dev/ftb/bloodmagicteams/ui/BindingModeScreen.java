package dev.ftb.bloodmagicteams.ui;

import dev.ftb.bloodmagicteams.BloodMagicTeams;
import dev.ftb.bloodmagicteams.data.PlayerBindingData.BindingMode;
import dev.ftb.bloodmagicteams.network.BindingModePayload;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * FTBLib-based UI screen for selecting binding mode (personal vs team).
 */
public class BindingModeScreen extends BaseScreen {
    private final String teamName;
    private boolean dontAskAgain = false;

    public BindingModeScreen(String teamName) {
        this.teamName = teamName;
    }

    @Override
    public boolean onInit() {
        setWidth(220);
        setHeight(160);
        return true;
    }

    @Override
    public void addWidgets() {
        // Title
        add(new TitlePanel(this));

        // Personal binding button
        add(new BindingButton(this, BindingMode.PERSONAL, 30, 45));

        // Team binding button
        add(new BindingButton(this, BindingMode.TEAM, 30, 75));

        // Don't ask again checkbox
        add(new CheckboxButton(this, 30, 110));

        // Warning text (only visible when checkbox is checked)
        add(new WarningPanel(this));
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.bloodmagicteams.binding_mode.title");
    }

    private class TitlePanel extends Panel {
        public TitlePanel(Panel parent) {
            super(parent);
            setPosAndSize(0, 5, parent.width, 30);
        }

        @Override
        public void addWidgets() {
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawString(graphics,
                    Component.translatable("gui.bloodmagicteams.binding_mode.title"),
                    x + w / 2, y + 5, Theme.CENTERED);
            theme.drawString(graphics,
                    Component.translatable("gui.bloodmagicteams.binding_mode.team", teamName),
                    x + w / 2, y + 18, Theme.CENTERED);
        }
    }

    private class BindingButton extends Button {
        private final BindingMode mode;

        public BindingButton(Panel parent, BindingMode mode, int x, int y) {
            super(parent);
            this.mode = mode;
            setPosAndSize(x, y, 160, 24);
        }

        @Override
        public Component getTitle() {
            return mode == BindingMode.PERSONAL
                    ? Component.translatable("gui.bloodmagicteams.binding_mode.personal")
                    : Component.translatable("gui.bloodmagicteams.binding_mode.team_button");
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            if (mode == BindingMode.PERSONAL) {
                list.add(Component.translatable("gui.bloodmagicteams.binding_mode.personal.tooltip"));
            } else {
                list.add(Component.translatable("gui.bloodmagicteams.binding_mode.team.tooltip"));
            }
        }

        @Override
        public void onClicked(MouseButton button) {
            if (button.isLeft()) {
                playClickSound();
                PacketDistributor.sendToServer(new BindingModePayload(mode, dontAskAgain));
                closeGui();
                BloodMagicTeams.LOGGER.debug("Selected binding mode: {} (dontAsk: {})", mode, dontAskAgain);
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            theme.drawButton(graphics, x, y, w, h, getWidgetType());
            theme.drawString(graphics, getTitle(), x + w / 2, y + (h - 8) / 2, Theme.CENTERED);
        }
    }

    private class CheckboxButton extends Button {
        public CheckboxButton(Panel parent, int x, int y) {
            super(parent);
            setPosAndSize(x, y, 160, 14);
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.bloodmagicteams.binding_mode.dont_ask");
        }

        @Override
        public void addMouseOverText(TooltipList list) {
            list.add(Component.translatable("gui.bloodmagicteams.binding_mode.dont_ask.tooltip"));
        }

        @Override
        public void onClicked(MouseButton button) {
            if (button.isLeft()) {
                playClickSound();
                dontAskAgain = !dontAskAgain;
            }
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            // Draw checkbox
            int boxSize = 10;
            int boxX = x;
            int boxY = y + (h - boxSize) / 2;

            // Checkbox border
            graphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFFFFFFFF);
            graphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, 0xFF000000);

            // Checkmark if checked
            if (dontAskAgain) {
                graphics.fill(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + boxSize - 2, 0xFF00FF00);
            }

            // Label
            theme.drawString(graphics, getTitle(), x + boxSize + 5, y + (h - 8) / 2);
        }
    }

    private class WarningPanel extends Panel {
        public WarningPanel(Panel parent) {
            super(parent);
            setPosAndSize(10, 128, parent.width - 20, 24);
        }

        @Override
        public void addWidgets() {
        }

        @Override
        public void alignWidgets() {
        }

        @Override
        public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (dontAskAgain) {
                Component warning = Component.translatable("gui.bloodmagicteams.binding_mode.reset_hint")
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
                theme.drawString(graphics, warning, x + w / 2, y + 2, Color4I.rgb(0xFFFF55), Theme.CENTERED);
            }
        }
    }
}
