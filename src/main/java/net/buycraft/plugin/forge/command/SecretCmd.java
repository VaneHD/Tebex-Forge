package net.buycraft.plugin.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.buycraft.plugin.BuyCraftAPI;
import net.buycraft.plugin.data.responses.ServerInformation;
import net.buycraft.plugin.forge.BuycraftPlugin;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;

import java.io.IOException;

public class SecretCmd implements Command<CommandSource> {
    private final BuycraftPlugin plugin;

    public SecretCmd(final BuycraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        if (context.getSource().getEntity() != null) {
            ForgeMessageUtil.sendMessage(context.getSource(), new StringTextComponent(ForgeMessageUtil.format(plugin,"secret_console_only"))
                    .setStyle(BuycraftPlugin.ERROR_STYLE));
            return 0;
        }

        String secret = StringArgumentType.getString(context, "secret");
        plugin.getPlatform().executeAsync(() -> {
            String currentKey = plugin.getConfiguration().getServerKey();
            BuyCraftAPI client = BuyCraftAPI.create(secret, plugin.getHttpClient());

            try {
                plugin.updateInformation(client);
            } catch (IOException e) {
                plugin.getLogger().error("Unable to verify secret", e);
                ForgeMessageUtil.sendMessage(context.getSource(), new StringTextComponent(ForgeMessageUtil.format(plugin,"secret_does_not_work"))
                        .setStyle(BuycraftPlugin.ERROR_STYLE));
                return;
            }

            ServerInformation information = plugin.getServerInformation();
            plugin.setApiClient(client);
            plugin.getConfiguration().setServerKey(secret);

            try {
                plugin.saveConfiguration();
            } catch (IOException e) {
                ForgeMessageUtil.sendMessage(context.getSource(), new StringTextComponent(ForgeMessageUtil.format(plugin,"secret_cant_be_saved"))
                        .setStyle(BuycraftPlugin.ERROR_STYLE));
            }

            ForgeMessageUtil.sendMessage(context.getSource(), new StringTextComponent(ForgeMessageUtil.format(plugin, "secret_success",
                    information.getServer().getName(), information.getAccount().getName())).setStyle(BuycraftPlugin.SUCCESS_STYLE));

            boolean repeatChecks = false;
            if (currentKey.equals("INVALID")) {
                repeatChecks = true;
            }

            plugin.getDuePlayerFetcher().run(repeatChecks);
        });
        return 1;
    }
}
