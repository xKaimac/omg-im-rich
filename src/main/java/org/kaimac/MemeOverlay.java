package org.kaimac;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MemeOverlay extends Overlay {
    private final Client client;

    private final List<String> audioClips = List.of(
            "src/main/resources/memes/explosion.wav",
            "src/main/resources/memes/bruh.wav",
            "src/main/resources/memes/kaching.wav",
            "src/main/resources/memes/money.wav",
            "src/main/resources/memes/wow.wav",
            "src/main/resources/memes/stonks.wav"
    );

    private final List<String> gifPaths = List.of(
            "src/main/resources/memes/boom.gif",
            "src/main/resources/memes/buy_buy_buy.gif",
            "src/main/resources/memes/sell_sell_sell.gif",
            "src/main/resources/memes/money.gif",
            "src/main/resources/memes/pikachu.gif",
            "src/main/resources/memes/stonks.gif"
    );

    private final Random random = new Random();

    private String message = "";
    private BufferedImage memeImage;
    private Clip currentClip;

    private int x = 50, y = 50;
    private int dx = 2, dy = 2;
    private final int IMAGE_WIDTH = 150;
    private final int IMAGE_HEIGHT = 150;

    @Inject
    public MemeOverlay(Client client) {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGHEST);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!message.isEmpty() && memeImage != null) {
            graphics.setFont(new Font("Comic Sans MS", Font.BOLD, 28));
            graphics.setColor(Color.YELLOW);
            graphics.drawString(message, x, y - 10);
            graphics.drawImage(memeImage, x, y, IMAGE_WIDTH, IMAGE_HEIGHT, null);

            // Bounce around screen bounds (1280x720 is a rough safe bound)
            x += dx;
            y += dy;

            if (x < 0 || x + IMAGE_WIDTH > 1280) dx *= -1;
            if (y < 0 || y + IMAGE_HEIGHT > 720) dy *= -1;
        }
        return null;
    }

    public void setMessage(String message) {
        this.message = message;
        showRandomMeme();
    }

    public void clearMessage() {
        this.message = "";
        this.memeImage = null;
        stopCurrentAudio();
    }

    public void showRandomMeme() {
        try {
            String imgPath = gifPaths.get(random.nextInt(gifPaths.size()));
            memeImage = ImageIO.read(new File(imgPath));
        } catch (IOException e) {
            System.err.println("Failed to load image: " + e.getMessage());
        }

        try {
            stopCurrentAudio();
            String audioPath = audioClips.get(random.nextInt(audioClips.size()));
            File audioFile = new File(audioPath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            currentClip = AudioSystem.getClip();
            currentClip.open(audioStream);
            currentClip.start();
        } catch (Exception e) {
            System.err.println("Failed to play audio: " + e.getMessage());
        }
    }

    private void stopCurrentAudio() {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
            currentClip.close();
        }
    }
}
