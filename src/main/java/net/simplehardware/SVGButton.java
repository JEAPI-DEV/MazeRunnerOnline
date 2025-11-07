package net.simplehardware;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;

import javax.swing.*;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class SVGButton extends JButton {
    
    private static final int DEFAULT_ICON_SIZE = 24;
    private final String svgResourcePath;
    private final int iconSize;
    private final String buttonText;
    
    public SVGButton(String text, String svgResourcePath) {
        this(text, svgResourcePath, DEFAULT_ICON_SIZE);
    }
    
    public SVGButton(String text, String svgResourcePath, int iconSize) {
        this(text, svgResourcePath, iconSize, text);
    }
    
    public SVGButton(String text, String svgResourcePath, int iconSize, String tooltip) {
        super();
        this.svgResourcePath = svgResourcePath;
        this.iconSize = iconSize;
        this.buttonText = text;
        setUpButton();
        setToolTipText(tooltip);
        loadSVGIcon();
    }
    
    private void setUpButton() {
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setPreferredSize(new Dimension(120, 40));
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setIconTextGap(8);
    }
    
    private void loadSVGIcon() {
        try {
            URL resourceUrl = this.getClass().getResource(svgResourcePath);
            if (resourceUrl == null) {
                System.err.println("Could not find SVG resource: " + svgResourcePath);
                setText(buttonText);
                return;
            }

            try (InputStream inputStream = resourceUrl.openStream()) {
                PNGTranscoder transcoder = new PNGTranscoder();
                transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) iconSize);
                transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) iconSize);

                TranscoderInput input = new TranscoderInput(inputStream);
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                TranscoderOutput output = new TranscoderOutput(outputStream);

                transcoder.transcode(input, output);
                byte[] pngData = outputStream.toByteArray();
                java.io.ByteArrayInputStream imageInputStream = new java.io.ByteArrayInputStream(pngData);
                BufferedImage bufferedImage = javax.imageio.ImageIO.read(imageInputStream);
                
                if (bufferedImage != null) {
                    ImageIcon icon = new ImageIcon(bufferedImage);
                    setIcon(icon);
                    System.out.println("Successfully loaded SVG icon from " + svgResourcePath);
                } else {
                    System.err.println("Failed to render SVG to image: " + svgResourcePath);
                    setText(buttonText);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error loading SVG icon from " + svgResourcePath + ": " + e.getMessage());
            e.printStackTrace();
            setText(buttonText);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isRollover()) {
            g2d.setColor(new Color(0, 0, 0, 20));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }

        if (getModel().isPressed()) {
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        setUpButton();
    }
}