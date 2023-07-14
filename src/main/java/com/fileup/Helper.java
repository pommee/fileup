package com.fileup;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

public class Helper {

    public static ImageView findImage(File file) {
        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        WritableImage writableImage = new WritableImage(width, height);
        SwingFXUtils.toFXImage(bufferedImage, writableImage);
        ImageView imageIcon = new javafx.scene.image.ImageView();
        imageIcon.setImage(writableImage);
        return imageIcon;
    }

}
