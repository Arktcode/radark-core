package radark;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.SpriteBatch;
import arc.graphics.g2d.TextureAtlas;
import arc.graphics.g2d.TextureAtlas.AtlasRegion;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.IntMap;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.io.Reads;
import arc.util.serialization.Base64Coder;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.Version;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.io.SaveFileReader;
import mindustry.io.TypeIO;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.legacy.LegacyBlock;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.InflaterInputStream;

public class SchematicRenderer {

    private final Color co = new Color();
    private Graphics2D currentGraphics;
    private BufferedImage currentImage;
    private final ObjectMap<String, Fi> imageFiles = new ObjectMap<>();
    private final ObjectMap<arc.graphics.Texture, BufferedImage> pageImages = new ObjectMap<>();

    public SchematicRenderer() {
        Version.enabled = false;
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();

        for (ContentType type : ContentType.all) {
            for (Content content : Vars.content.getBy(type)) {
                try {
                    content.init();
                } catch (Throwable ignored) {
                }
            }
        }

        String assetsPath = System.getProperty("assets.path", "./assets");
        Vars.state = new GameState();

        Fi assetsDir = new Fi(assetsPath);

        Fi atlasFile = assetsDir.child("sprites/sprites.aatls");

        if (!atlasFile.exists()) {
            throw new RuntimeException(
                    "ERROR KATASTROFICO: No se encuenta el archivo 'assets/sprites/sprites.aatls'.\n" +
                            "Asegúrate de subir la carpeta 'assets' completa al mismo lugar que el .jar!");
        }

        Fi spritesDir = assetsDir.child("sprites");

        TextureAtlasData data = new TextureAtlasData(atlasFile, spritesDir, false);
        Core.atlas = new TextureAtlas();

        data.getPages().each(page -> {
            page.texture = arc.graphics.Texture.createEmpty(null);
            page.texture.width = page.width;
            page.texture.height = page.height;

            try {
                BufferedImage img = ImageIO.read(page.textureFile.file());
                pageImages.put(page.texture, img);
            } catch (Exception e) {
            }
        });

        data.getRegions().each(reg -> {
            AtlasRegion region = new AtlasRegion(reg.page.texture, reg.left, reg.top, reg.width, reg.height);
            region.name = reg.name;
            region.texture = reg.page.texture;
            Core.atlas.addRegion(reg.name, region);
        });

        Lines.useLegacyLine = true;
        Core.atlas.setErrorRegion("error");
        Draw.scl = 1f / 4f;

        Core.batch = new SpriteBatch(0) {
            @Override
            protected void draw(arc.graphics.g2d.TextureRegion region, float x, float y, float originX, float originY,
                    float width, float height, float rotation) {
                x += 4;
                y += 4;
                x *= 4;
                y *= 4;
                width *= 4;
                height *= 4;
                y = currentImage.getHeight() - (y + height / 2f) - height / 2f;
                AffineTransform at = new AffineTransform();
                at.translate(x, y);
                at.rotate(-rotation * Mathf.degRad, originX * 4, originY * 4);
                currentGraphics.setTransform(at);
                BufferedImage image = getImage(((AtlasRegion) region).name);
                if (!color.equals(Color.white)) {
                    image = tint(image, color);
                }
                currentGraphics.drawImage(image, 0, 0, (int) width, (int) height, null);
            }

            @Override
            protected void draw(arc.graphics.Texture texture, float[] spriteVertices, int offset, int count) {
            }
        };

        for (ContentType type : ContentType.values()) {
            for (Content content : Vars.content.getBy(type)) {
                try {
                    content.load();
                    content.loadIcon();
                } catch (Throwable ignored) {
                }
            }
        }

        try {
            BufferedImage image = ImageIO.read(assetsDir.child("sprites/block_colors.png").file());
            for (Block block : Vars.content.blocks()) {
                block.mapColor.argb8888(image.getRGB(block.id, 0));
                if (block instanceof OreBlock) {
                    block.mapColor.set(((OreBlock) block).itemDrop.color);
                }
            }
        } catch (Exception e) {
        }

        Vars.world = new mindustry.core.World() {
            public Tile tile(int x, int y) {
                return new Tile(x, y);
            }
        };
    }

    private BufferedImage getImage(String name) {
        try {
            AtlasRegion region = Core.atlas.find(name);
            if (region != null && region.found()) {
                BufferedImage pageImage = pageImages.get(region.texture);
                if (pageImage != null) {
                    int x = (int) (region.u * pageImage.getWidth());
                    int y = (int) (region.v * pageImage.getHeight());
                    return pageImage.getSubimage(x, y, region.width, region.height);
                }
            }
            return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        } catch (Exception e) {
            return new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        }
    }

    private BufferedImage tint(BufferedImage image, Color color) {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Color tmp = new Color();
        for (int x = 0; x < copy.getWidth(); x++) {
            for (int y = 0; y < copy.getHeight(); y++) {
                int argb = image.getRGB(x, y);
                tmp.argb8888(argb);
                tmp.mul(color);
                copy.setRGB(x, y, tmp.argb8888());
            }
        }
        return copy;
    }

    public Schematic parseSchematic(String base64) throws IOException {
        return readSchematic(new ByteArrayInputStream(Base64Coder.decode(base64)));
    }

    private static Schematic readSchematic(InputStream input) throws IOException {
        byte[] header = { 'm', 's', 'c', 'h' };
        for (byte b : header) {
            if (input.read() != b) {
                throw new IOException("Esto no es un esquema (missing 'msch' header)");
            }
        }
        input.read();

        try (DataInputStream stream = new DataInputStream(new InflaterInputStream(input))) {
            short width = stream.readShort();
            short height = stream.readShort();

            StringMap map = new StringMap();
            byte tags = stream.readByte();
            for (int i = 0; i < tags; i++) {
                map.put(stream.readUTF(), stream.readUTF());
            }

            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for (int i = 0; i < length; i++) {
                String name = stream.readUTF();
                Block block = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
                blocks.put(i, block == null || block instanceof LegacyBlock ? Blocks.air : block);
            }

            int total = stream.readInt();
            if (total > 64 * 64) {
                throw new IOException("Esquema demasiado grande");
            }

            Seq<Stile> tiles = new Seq<>(total);
            for (int i = 0; i < total; i++) {
                Block block = blocks.get(stream.readByte());
                int position = stream.readInt();
                Object config = TypeIO.readObject(Reads.get(stream));
                byte rotation = stream.readByte();
                if (block != Blocks.air) {
                    tiles.add(new Stile(block, Point2.x(position), Point2.y(position), config, rotation));
                }
            }

            return new Schematic(tiles, map, width, height);
        }
    }

    public BufferedImage renderSchematic(Schematic schematic) throws IOException {
        if (schematic.width > 64 || schematic.height > 64) {
            throw new IOException("Esquema demasiado grande");
        }

        BufferedImage image = new BufferedImage(
                schematic.width * 32,
                schematic.height * 32,
                BufferedImage.TYPE_INT_ARGB);

        Draw.reset();

        Seq<BuildPlan> requests = schematic.tiles
                .map(t -> new BuildPlan(t.x, t.y, t.rotation, t.block, t.config));

        currentGraphics = image.createGraphics();
        currentImage = image;

        requests.each(req -> {
            req.animScale = 1f;
            req.worldContext = false;
            req.block.drawPlanRegion(req, requests);
            Draw.reset();
        });

        requests.each(req -> req.block.drawPlanConfigTop(req, requests));

        currentGraphics.dispose();
        return image;
    }
}
