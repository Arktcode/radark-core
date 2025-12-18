package radark;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.content.Blocks;
import mindustry.ctype.ContentType;
import mindustry.game.Schematic;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class Stats {

    public static final Map<String, String> EMOTES = new HashMap<>();

    static {

        // Configuracion de emojis del embed. algunos no tienen temporalmente...
        // Items Serpulo
        EMOTES.put("copper", "<:cobre:1162829987989172254>");
        EMOTES.put("lead", "<:plomo:1162830193514250350>");
        EMOTES.put("metaglass", "<:metacristal:1330600746856484884>");
        EMOTES.put("graphite", "<:grafito:1162830084982448198>");
        EMOTES.put("sand", "<:arena:1330600753777082449>");
        EMOTES.put("coal", "<:carbon:1330600745333952596>");
        EMOTES.put("titanium", "<:titanio:1162830272891461662>");
        EMOTES.put("thorium", "<:torio:1200964495397363802>");
        EMOTES.put("scrap", "<:chatarra:1330600754993692713>");
        EMOTES.put("silicon", "<:silicio:1182532671642021889>");
        EMOTES.put("plastanium", "<:plastanio:1167316708961292298>");
        EMOTES.put("phase-fabric", "<:aleacionelctrica:1162833334884368384>");
        EMOTES.put("surge-alloy", "<:aleacionelctrica:1162833334884368384>");
        EMOTES.put("spore-pod", "<:vainadeesporas:1397917969341743218>");
        EMOTES.put("blast-compound", "<:compuestoexplosivo:1167316380245299200>");
        EMOTES.put("pyratite", "<:pirotita:1330600751785054298>");
        EMOTES.put("phase-fabric", "<:Tejidofase:1294135131078000650>");

        // Items Erekir
        EMOTES.put("beryllium", "<:berilio:1176737469983641671>");
        EMOTES.put("tungsten", "<:tungsteno:1306707215880683590>");
        EMOTES.put("carbide", "<:carburo:1306707190408679475>");
        EMOTES.put("oxide", "<:oxido:1211824023680585758>");

        // Stats
        EMOTES.put("power-plus", "⚡+");
        EMOTES.put("power-minus", "⚡-");
        EMOTES.put("liquid", "💧");
        EMOTES.put("heat", "🔥");

        // Liquidos
        EMOTES.put("water", "<:Agua:1200962052030730450>");
        EMOTES.put("oil", "<:petroleo:1200963716884201502>");
        EMOTES.put("cryofluid", "<:criogenico:1382931942638616616>");
        EMOTES.put("slag", "<:Fundido:1200961953888211035>");
        EMOTES.put("arkycite", "<:Arkycita:1200962441278935093>");
        EMOTES.put("ozone", "🟣");
        EMOTES.put("hidrogen", "⚪");
        EMOTES.put("nitrogen", "☁️");
        EMOTES.put("cyanogen", "🔵");
    }

    public static EmbedBuilder getEmbed(Schematic schematic) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.decode("#fed17b"));

        // Calcula el costo
        Map<Item, Integer> costs = new HashMap<>();
        schematic.tiles.each(tile -> {
            if (tile.block.requirements != null) {
                for (ItemStack stack : tile.block.requirements) {
                    costs.merge(stack.item, stack.amount, Integer::sum);
                }
            }
        });

        StringBuilder costStr = new StringBuilder();
        costs.forEach((item, amount) -> {
            String emoji = EMOTES.getOrDefault(item.name, "");
            costStr.append(emoji).append(" **").append(amount).append("**  ");
        });

        if (costStr.length() > 0) {
            embed.setDescription(costStr.toString());
        }

        // Calcula las estadisticas
        float powerProd = 0f;
        float powerCons = 0f;

        for (Schematic.Stile tile : schematic.tiles) {
            Block block = tile.block;

            if (block.consPower != null) {
                powerCons += block.consPower.usage * 60f;
            }
            if (block.outputsPower && !block.name.equals("power-source") && !block.name.equals("infinite-power-node")) {
                try {
                    java.lang.reflect.Field field = block.getClass().getField("powerProduction");
                    float production = field.getFloat(block) * 60f;
                    if (production < 1000000f) {
                        powerProd += production;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (powerProd > 0.001f)
            embed.addField(EMOTES.get("power-plus") + " Generación", String.format("%.1f/s", powerProd), true);
        if (powerCons > 0.001f)
            embed.addField(EMOTES.get("power-minus") + " Consumo", String.format("-%.1f/s", powerCons), true);

        // Liquid Stats :D
        Map<String, Float> liquidProd = new HashMap<>();
        Map<String, Float> liquidCons = new HashMap<>();

        java.util.function.BiConsumer<Map<String, Float>, String> addProd = (map, name) -> map.merge(name, 60f,
                Float::sum);

        for (Schematic.Stile tile : schematic.tiles) {
            Block block = tile.block;

            try {
                if (block.consumers != null) {
                    for (mindustry.world.consumers.Consume cons : block.consumers) {
                        if (cons instanceof mindustry.world.consumers.ConsumeLiquid) {
                            mindustry.world.consumers.ConsumeLiquid lc = (mindustry.world.consumers.ConsumeLiquid) cons;
                            liquidCons.merge(lc.liquid.name, lc.amount * 60f, Float::sum);
                        }
                    }
                }

                try {
                    java.lang.reflect.Field field = block.getClass().getField("outputLiquid");
                    mindustry.type.LiquidStack stack = (mindustry.type.LiquidStack) field.get(block);
                    if (stack != null) {
                        liquidProd.merge(stack.liquid.name, stack.amount * 60f, Float::sum);
                    }
                } catch (Exception ignored) {
                }
            } catch (Exception ignored) {
            }
        }

        liquidProd.forEach((name, amount) -> {
            if (amount > 0.001f) {
                String emoji = EMOTES.getOrDefault(name, EMOTES.get("liquid"));
                embed.addField(emoji + " " + name, String.format("+%.1f/s", amount), true);
            }
        });

        liquidCons.forEach((name, amount) -> {
            if (amount > 0.001f) {
                String emoji = EMOTES.getOrDefault(name, EMOTES.get("liquid"));
                embed.addField(emoji + " " + name, String.format("-%.1f/s", amount), true);
            }
        });

        return embed;
    }
}
