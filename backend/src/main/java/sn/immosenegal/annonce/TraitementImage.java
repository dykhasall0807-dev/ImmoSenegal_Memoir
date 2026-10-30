package sn.immosenegal.annonce;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Nettoyage d'une photo reçue : on la décode puis on la ré-encode à neuf.
 * Effets : le fichier est une vraie image (sinon refus), toutes les métadonnées sont perdues (dont la position GPS
 * des photos de téléphone), et l'image est réduite à {@value #COTE_MAX} px au plus.
 */
final class TraitementImage {

    static final int COTE_MAX = 1600;
    /** Garde-fou contre les « bombes de décompression » : petit fichier, mais des milliards de pixels. */
    static final long PIXELS_MAX = 25_000_000L;

    record Resultat(byte[] octets, String typeMime) {
    }

    /** Erreur de contenu (image illisible, trop grande...) : le message est destiné à l'utilisateur. */
    static final class ImageRefusee extends Exception {
        ImageRefusee(String message) {
            super(message);
        }
    }

    private TraitementImage() {
    }

    static Resultat nettoyer(byte[] source, String typeMime) throws ImageRefusee {
        BufferedImage image = decoder(source);
        image = reduire(image);
        try {
            return "image/png".equals(typeMime)
                ? new Resultat(ecrirePng(image), "image/png")
                : new Resultat(ecrireJpeg(image), "image/jpeg");
        } catch (IOException e) {
            throw new ImageRefusee("Image impossible à traiter.");
        }
    }

    private static BufferedImage decoder(byte[] source) throws ImageRefusee {
        try (ImageInputStream flux = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
            if (!lecteurs.hasNext()) {
                throw new ImageRefusee("Ce fichier n'est pas une image lisible.");
            }
            ImageReader lecteur = lecteurs.next();
            try {
                lecteur.setInput(flux, true, true);
                // Les dimensions sont lues AVANT de décoder les pixels.
                long pixels = (long) lecteur.getWidth(0) * lecteur.getHeight(0);
                if (pixels <= 0 || pixels > PIXELS_MAX) {
                    throw new ImageRefusee("Image trop grande (plus de 25 millions de pixels).");
                }
                return lecteur.read(0);
            } finally {
                lecteur.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new ImageRefusee("Ce fichier n'est pas une image lisible.");
        }
    }

    private static BufferedImage reduire(BufferedImage image) {
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        int plusGrand = Math.max(largeur, hauteur);
        if (plusGrand <= COTE_MAX) {
            return image;
        }
        double echelle = (double) COTE_MAX / plusGrand;
        int l = Math.max(1, (int) Math.round(largeur * echelle));
        int h = Math.max(1, (int) Math.round(hauteur * echelle));
        int type = image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage reduite = new BufferedImage(l, h, type);
        Graphics2D g = reduite.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(image, 0, 0, l, h, null);
        } finally {
            g.dispose();
        }
        return reduite;
    }

    private static byte[] ecrirePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", sortie)) {
            throw new IOException("Pas d'encodeur PNG");
        }
        return sortie.toByteArray();
    }

    private static byte[] ecrireJpeg(BufferedImage image) throws IOException {
        // Le JPEG n'a pas de transparence : on aplatit sur fond blanc.
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        ImageWriter ecrivain = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam parametres = ecrivain.getDefaultWriteParam();
            parametres.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametres.setCompressionQuality(0.85f);
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream flux = new MemoryCacheImageOutputStream(sortie)) {
                ecrivain.setOutput(flux);
                // IIOImage sans métadonnées : rien n'est recopié de l'original.
                ecrivain.write(null, new IIOImage(rgb, null, null), parametres);
            }
            return sortie.toByteArray();
        } finally {
            ecrivain.dispose();
        }
    }
}
