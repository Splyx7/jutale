package dev.spotifyoverlay;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class Vinyl {
    public static final int S = 128;

    public static BufferedImage bake(BufferedImage label) {
        BufferedImage o = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double k = S / 2.0 / 30.0, c = S / 2.0;

        g.setColor(new Color(0x0D0D0D));
        g.fill(new Ellipse2D.Double(0, 0, S, S));
        g.setColor(new Color(255, 255, 255, 36));
        g.setStroke(new BasicStroke((float) (0.6 * k)));
        for (double r = 8; r < 29; r += 3.5)
            g.draw(new Ellipse2D.Double(c - r * k, c - r * k, 2 * r * k, 2 * r * k));
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(0.8, 0.8, S - 1.6, S - 1.6));

        double lr = 17 * k;
        Shape clip = new Ellipse2D.Double(c - lr, c - lr, 2 * lr, 2 * lr);
        Shape old = g.getClip();
        g.setClip(clip);
        if (label != null) {
            int w = label.getWidth(), h = label.getHeight(), m = Math.min(w, h);
            g.drawImage(label, (int) (c - lr), (int) (c - lr), (int) (c + lr), (int) (c + lr),
                    (w - m) / 2, (h - m) / 2, (w + m) / 2, (h + m) / 2, null);
        } else {
            g.setColor(new Color(0x2A2A2A));
            g.fill(clip);
        }
        g.setClip(old);
        g.setColor(new Color(0, 0, 0, 170));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(clip);

        double h2 = 2.5 * k;
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(c - h2, c - h2, 2 * h2, 2 * h2));
        g.dispose();
        return o;
    }
}
