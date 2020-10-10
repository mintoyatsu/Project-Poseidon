package org.bukkit.command.defaults;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TpsCommand extends Command {
    private final Server server;

    public TpsCommand(Server server) {
        super("tps");
        this.server = server;
        this.description = "Obtains the current server TPS rate.";
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        String str = ChatColor.GOLD + "TPS over the last 1, 5, 15 minutes: ";

        double[] tps = server.getTPS();

        DecimalFormat fmt = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        str += colorTpsValue(tps[0]) + fmt.format(tps[0]) + ChatColor.GOLD + ", ";
        str += colorTpsValue(tps[1]) + fmt.format(tps[1]) + ChatColor.GOLD + ", ";
        str += colorTpsValue(tps[2]) + fmt.format(tps[2]);

        sender.sendMessage(str);
        return true;
    }

    private ChatColor colorTpsValue(double val) {
        if (Double.compare(val, 19.0d) >= 0) {
            // Excellent - Your server is performing well.
            return ChatColor.GREEN;
        } else if (Double.compare(val, 17.0d) >= 0) {
            // Fair - Your server is lagging somewhat
            return ChatColor.YELLOW;
        } else {
            // Unplayable
            return ChatColor.RED;
        }
    }
}
