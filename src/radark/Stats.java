package radark;

import mindustry.game.Schematic;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.LoggerFactory;

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
        EMOTES.put("ozone", "<:ozone:1487622364601778196>");
        EMOTES.put("hidrogen", "<:hydrogen:1487622275539931147> ");
        EMOTES.put("nitrogen", "<:nitrogen:1487622225115877496>");
        EMOTES.put("cyanogen", "<:cyanogen:1487622322595565738>");
    }

    public static EmbedBuilder getEmbed(Schematic schematic) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.decode("#fed17b"));

        try {
            // Field 1: Recursos (costo de materiales)
            Map<Item, Integer> costs = new HashMap<>();
            if (schematic.tiles != null) {
                for (Schematic.Stile tile : schematic.tiles) {
                    if (tile.block != null && tile.block.requirements != null) {
                        for (ItemStack stack : tile.block.requirements) {
                            if (stack.item != null) {
                                costs.merge(stack.item, stack.amount, Integer::sum);
                            }
                        }
                    }
                }
            }

            StringBuilder costStr = new StringBuilder();
            costs.forEach((item, amount) -> {
                String emoji = EMOTES.getOrDefault(item.name, "");
                costStr.append(emoji).append(" **").append(amount).append("**  ");
            });

            if (costStr.length() > 0) {
                embed.addField("Recursos", cap(costStr.toString()), false);
            }

            // Field 2: Consumo (energía + líquidos)
            StringBuilder consumoStr = new StringBuilder();

            float powerProd = 0f;
            float powerCons = 0f;

            if (schematic.tiles != null) {
                for (Schematic.Stile tile : schematic.tiles) {
                    Block block = tile.block;
                    if (block == null)
                        continue;

                    if (block.consPower != null) {
                        powerCons += block.consPower.usage * 60f;
                    }
                    if (block.outputsPower && !block.name.equals("power-source")
                            && !block.name.equals("infinite-power-node")) {
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
            }

            if (powerProd > 0.001f)
                consumoStr.append(EMOTES.get("power-plus")).append(" **").append(formatValue(powerProd))
                        .append("/s**  ");
            if (powerCons > 0.001f)
                consumoStr.append(EMOTES.get("power-minus")).append(" **").append(formatValue(powerCons))
                        .append("/s**  ");

            // Liquidos
            Map<String, Float> liquidProd = new HashMap<>();
            Map<String, Float> liquidCons = new HashMap<>();

            if (schematic.tiles != null) {
                for (Schematic.Stile tile : schematic.tiles) {
                    Block block = tile.block;
                    if (block == null)
                        continue;
                    try {
                        if (block.consumers != null) {
                            for (mindustry.world.consumers.Consume cons : block.consumers) {
                                if (cons instanceof mindustry.world.consumers.ConsumeLiquid) {
                                    mindustry.world.consumers.ConsumeLiquid lc = (mindustry.world.consumers.ConsumeLiquid) cons;
                                    if (lc.liquid != null) {
                                        liquidCons.merge(lc.liquid.name, lc.amount * 60f, Float::sum);
                                    }
                                }
                            }
                        }
                        try {
                            java.lang.reflect.Field field = block.getClass().getField("outputLiquid");
                            mindustry.type.LiquidStack stack = (mindustry.type.LiquidStack) field.get(block);
                            if (stack != null && stack.liquid != null) {
                                liquidProd.merge(stack.liquid.name, stack.amount * 60f, Float::sum);
                            }
                        } catch (Exception ignored) {
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            liquidProd.forEach((name, amount) -> {
                if (amount > 0.001f) {
                    String emoji = EMOTES.getOrDefault(name, EMOTES.get("liquid"));
                    consumoStr.append(emoji).append(" **+").append(String.format("%.1f/s", amount)).append("**  ");
                }
            });

            liquidCons.forEach((name, amount) -> {
                if (amount > 0.001f) {
                    String emoji = EMOTES.getOrDefault(name, EMOTES.get("liquid"));
                    consumoStr.append(emoji).append(" **-").append(String.format("%.1f/s", amount)).append("**  ");
                }
            });

            if (consumoStr.length() > 0) {
                embed.addField("Stats", cap(consumoStr.toString()), false);
            }

            // Tags (Zona consolidada)
            if (schematic.tags != null) {
                StringBuilder tagsSb = new StringBuilder();
                
                // Añadir dimensiones al principio de los tags
                tagsSb.append("• **Dimensiones**: ").append(schematic.width).append("x").append(schematic.height).append("\n");

                schematic.tags.each((key, value) -> {
                    // Solo mostrar tags secundarios (no estructurales)
                    if (!key.equals("name") && !key.equals("description") && !key.equals("author")) {
                        String cleanVal = value.replace("[", "").replace("]", "").replace("\"", "").replace(",", ", ");
                        tagsSb.append("• **").append(key).append("**: ").append(cleanVal).append("\n");
                    }
                });

                if (tagsSb.length() > 0) {
                    embed.addField("Tags", tagsSb.toString(), false);
                }

                // Descripción (Footer)
                String desc = schematic.tags.get("description");
                if (desc != null && !desc.trim().isEmpty()) {
                    embed.setFooter(desc);
                }
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(Stats.class).error("Error generando estadisticas", e);
            embed.addField("Error", "No se pudieron calcular las estadísticas por completo.", false);
        }

        return embed;
    }
     //Trunca un string al límite de 1024 chars que Discord impone en field values.
    private static String cap(String s) {
        return s.length() <= 1024 ? s : s.substring(0, 1020) + "...";
    }

    /** Formatea valores grandes (k, M) para mejor legibilidad. */
    private static String formatValue(float value) {
        if (value >= 1_000_000)
            return String.format("%.1fM", value / 1_000_000f);
        if (value >= 1_000)
            return String.format("%.1fk", value / 1_000f);
        return String.format("%.1f", value);
    }
}
