package radark;

import io.github.cdimascio.dotenv.Dotenv;
import mindustry.game.Schematic;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.entities.Activity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

public class radArk extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(radArk.class);
    private final SchematicRenderer renderer;
    // originalId -> botResponseId (Límite de 100 para optimizar RAM)
    private final java.util.Map<Long, Long> messageMap = java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<Long, Long>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, Long> eldest) { return size() > 100; }
    });

    public radArk() { this.renderer = new SchematicRenderer(); }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String token = dotenv.get("DISCORD_TOKEN");
        if (token == null) { logger.error("Falta DISCORD_TOKEN"); System.exit(1); }

        try {
            radArk bot = new radArk();
            JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setActivity(Activity.watching("/schem"))
                    .addEventListeners(bot)
                    .build()
                    .updateCommands().addCommands(
                        Commands.slash("schem", "Renderiza un esquema")
                            .addOption(OptionType.ATTACHMENT, "file", "Para Archivos .msch / .schem")
                            .addOption(OptionType.STRING, "code", "Para códigos Base64")
                    ).queue();
            logger.info("Bot radArk v0.7.0 Operativo");
        } catch (Exception e) { logger.error("Fallo al iniciar", e); }
    }

    @Override
    public void onMessageDelete(@NotNull net.dv8tion.jda.api.events.message.MessageDeleteEvent event) {
        Long botMsgId = messageMap.remove(event.getMessageIdLong());
        if (botMsgId != null) event.getChannel().deleteMessageById(botMsgId).queue(null, e -> {});
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        Message msg = event.getMessage();
        boolean processed = false;

        // 1. Detección Stricta de Archivos
        if (!msg.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : msg.getAttachments()) {
                String ext = attachment.getFileName().toLowerCase();
                if (ext.endsWith(".msch") || ext.endsWith(".schem")) {
                    processSource(event.getChannel(), msg, attachment.getUrl(), attachment.getFileName());
                    processed = true;
                    break;
                }
            }
        }

        // 2. Detección por Código Base64 (Solo si no se procesó un archivo)
        if (!processed) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(bXNja[A-Za-z0-9+/=]{20,})").matcher(msg.getContentRaw());
            if (matcher.find()) {
                renderSchem(event.getChannel(), msg, matcher.group(1), "schematic.msch");
            }
        }
    }

    private void processSource(MessageChannel channel, Message userMsg, String url, String name) {
        channel.sendTyping().queue();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try (InputStream is = java.net.URI.create(url).toURL().openStream()) {
                String b64 = Base64.getEncoder().encodeToString(is.readAllBytes());
                renderSchem(channel, userMsg, b64, name);
            } catch (Exception e) { logger.error("Error al procesar fuente", e); }
        });
    }

    private void renderSchem(MessageChannel channel, Message userMsg, String b64, String fileName) {
        try {
            Schematic schematic = renderer.parseSchematic(b64);
            BufferedImage image = renderer.renderSchematic(schematic);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            EmbedBuilder eb = Stats.getEmbed(schematic);
            eb.setTitle(schematic.tags.get("name", fileName));
            eb.setImage("attachment://schem.png");

            channel.sendFiles(FileUpload.fromData(baos.toByteArray(), "schem.png")).setEmbeds(eb.build())
                    .queue(res -> { if (userMsg != null) messageMap.put(userMsg.getIdLong(), res.getIdLong()); });
        } catch (Exception e) { channel.sendMessage("Error renderizando esquema").queue(); }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("schem")) return;
        event.deferReply().queue(hook -> {
            try {
                OptionMapping f = event.getOption("file");
                OptionMapping c = event.getOption("code");
                if (f != null) {
                    try (InputStream is = java.net.URI.create(f.getAsAttachment().getUrl()).toURL().openStream()) {
                        renderSchemInteraction(event, Base64.getEncoder().encodeToString(is.readAllBytes()), f.getAsAttachment().getFileName());
                    }
                } else if (c != null) renderSchemInteraction(event, c.getAsString().trim(), "schematic.msch");
            } catch (Exception e) { event.getHook().sendMessage("Error").queue(); }
        });
    }

    private void renderSchemInteraction(SlashCommandInteractionEvent event, String b64, String name) throws Exception {
        Schematic s = renderer.parseSchematic(b64);
        BufferedImage img = renderer.renderSchematic(s);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);

        EmbedBuilder eb = Stats.getEmbed(s);
        eb.setTitle(s.tags.get("name", name)).setImage("attachment://schem.png");
        event.getHook().sendFiles(FileUpload.fromData(baos.toByteArray(), "schem.png")).addEmbeds(eb.build()).queue();
    }
}
