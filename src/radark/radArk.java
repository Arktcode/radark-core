package radark;

import io.github.cdimascio.dotenv.Dotenv;
import mindustry.game.Schematic;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.JDA;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import net.dv8tion.jda.api.entities.Activity;

public class radArk extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(radArk.class);
    private final SchematicRenderer renderer;

    /**
     * Bot de Esquemas .msch | base64 basado en Core para Discord.
     * 
     * @author Arksource
     * @version 0.6.4
     */
    public radArk() {
        logger.info("Empezando a renderizar esquemas...");
        this.renderer = new SchematicRenderer();
        logger.info("Render Completado exitosamente!");
    }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        String token = dotenv.get("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            logger.error("DISCORD_TOKEN no esta en tu entorno o archivo .env");
            System.exit(1);
        }

        try {
            radArk bot = new radArk();

            JDA jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES)
                    .setActivity(Activity.watching("/schem"))
                    .addEventListeners(bot)
                    .build();

            // esto registra comandos /Slash
            jda.updateCommands().addCommands(
                    Commands.slash("schem", "Renderiza un esquema de Mindustry")
                            .addOption(OptionType.ATTACHMENT, "file", "Para Archivos .msch")
                            .addOption(OptionType.STRING, "code", "Para codigos Base64"))
                    .queue();

            jda.awaitReady();
            logger.info("El bot se registro como: {}", jda.getSelfUser().getAsTag());
            logger.info("Servidores: {}", jda.getGuilds().size());

        } catch (Exception e) {
            logger.error("No se pudo iniciar el bot :(...", e);
            System.exit(1);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot())
            return;

        Message message = event.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = event.getChannel();

        boolean isMentioned = message.getMentions().isMentioned(event.getJDA().getSelfUser(), Message.MentionType.USER);

        // 1. Compruebe si hay archivos adjuntos .msch
        if (!message.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                if (attachment.getFileName().endsWith(".msch")) {
                    processAttachment(channel, attachment);
                    return;
                }
            }
        }

        // 2. Comprueba Base64 (regex comprueba si inicia con bXNja...)
        // - "@Bot bXNja..."
        // - "Comprueba esto: bXNja..."
        // - "bXNja..."
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(bXNja[A-Za-z0-9+/=]+)").matcher(content);
        if (matcher.find()) {
            String schemCode = matcher.group(1);
            processBase64(channel, schemCode, "schematic.msch");
            return;
        }

        // 3. Reserva los comandos (por si acaso...)
        if (content.startsWith("!schem")) {
            handleSchematicCommand(event);
            return;
        }

        if (content.equals("!help")) {
            sendHelp(channel);
        }
    }

    private void sendHelp(MessageChannel channel) {
        MessageEmbed embed = new EmbedBuilder()
                .setTitle("Mindustry Schematic Bot by: Arksource")
                .setDescription("Renderiza esquemas de Mindustry a imágenes")
                .setColor(0x6B6BFF)
                .addField("Comandos", "", false)
                .addField("`!schem [archivo]`",
                        "Renderiza un esquema\n• Adjunta un archivo `.msch`\n• O pega el código base64", false)
                .addField("`Auto-detect`", "Sube un archivo .msch o pega el código base64 y el bot lo detectará.",
                        false)
                .addField("`!help`", "Muestra este mensaje", false)
                .setFooter("Basado en el CoreBot de Anuken")
                .build();
        channel.sendMessageEmbeds(embed).queue();
    }

    private void handleSchematicCommand(MessageReceivedEvent event) {
        Message message = event.getMessage();
        MessageChannel channel = event.getChannel();
        String content = message.getContentRaw();

        if (!message.getAttachments().isEmpty()) {
            processAttachment(channel, message.getAttachments().get(0));
        } else {
            String[] parts = content.split("\\s+", 2);
            if (parts.length >= 2) {
                processBase64(channel, parts[1].trim(), "schematic.msch");
            } else {
                channel.sendMessage("Por favor adjunta un archivo `.msch` o pega el código base64").queue();
            }
        }
    }

    private void processAttachment(MessageChannel channel, Message.Attachment attachment) {
        try {
            logger.info("Descargando archivo adjunto: {}", attachment.getFileName());
            channel.sendTyping().queue();
            InputStream stream = attachment.getProxy().download().join();
            byte[] data = stream.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(data);
            processBase64(channel, base64, attachment.getFileName());
        } catch (Exception e) {
            logger.error("Error al descargar el archivo", e);
            channel.sendMessage("Error al descargar el archivo").queue();
        }
    }

    private void processBase64(MessageChannel channel, String base64, String fileName) {
        try {
            Schematic schematic = renderer.parseSchematic(base64);
            BufferedImage image = renderer.renderSchematic(schematic);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageData = baos.toByteArray();

            String name = schematic.tags.get("name", fileName);

            // construye el embed del bot a base de Stat.java
            EmbedBuilder embed = Stats.getEmbed(schematic);
            embed.setTitle(name);
            embed.setImage("attachment://schematic.png");

            channel.sendMessageEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(imageData, "schematic.png"))
                    .queue();
        } catch (Exception e) {
            logger.error("Error al renderizar", e);
            channel.sendMessage("Error al renderizar: " + e.getMessage()).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("schem"))
            return;

        event.deferReply().queue();

        OptionMapping fileOption = event.getOption("file");
        OptionMapping codeOption = event.getOption("code");

        String base64 = null;
        String fileName = "schematic.msch";

        // si el usuario envia un archivo .msch lo procesa aqui mismo :p
        if (fileOption != null) {
            Message.Attachment attachment = fileOption.getAsAttachment();
            fileName = attachment.getFileName();
            try {
                InputStream stream = attachment.getProxy().download().join();
                byte[] data = stream.readAllBytes();
                base64 = Base64.getEncoder().encodeToString(data);
            } catch (Exception e) {
                event.getHook().sendMessage("Error al descargar el archivo").queue();
                return;
            }
        } else if (codeOption != null) {
            base64 = codeOption.getAsString().trim();
        } else {
            event.getHook().sendMessage("Creo que deberias usar`file` o `code`! <:iracundo:1329832541737193513>")
                    .queue();
            return;
        }

        // Valida que el usuario no envie un codigo vacio
        if (base64 == null || base64.isEmpty()) {
            event.getHook().sendMessage("Uso invalido").queue();
            return;
        }

        // Parsea el esquema y lo renderiza :P...
        try {
            Schematic schematic = renderer.parseSchematic(base64);
            BufferedImage image = renderer.renderSchematic(schematic);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageData = baos.toByteArray();

            String name = schematic.tags.get("name", fileName);

            EmbedBuilder embed = Stats.getEmbed(schematic);
            embed.setTitle(name);
            embed.setImage("attachment://schematic.png");

            event.getHook().sendMessageEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(imageData, "schematic.png"))
                    .queue();

        } catch (Exception e) {
            event.getHook().sendMessage("Error: " + e.getMessage()).queue();
            logger.error("Error al usar un SlashCommand", e);
        }
    }
}
