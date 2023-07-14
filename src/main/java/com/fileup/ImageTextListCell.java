package com.fileup;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;

class ImageTextListCell extends ListCell<String> {

    private final Label textLabel;
    private final HBox hbox;

    public ImageTextListCell(File file) {
        // Create the ImageView and Label
        ImageView imageView = new ImageView();
        imageView.setFitWidth(20);
        imageView.setFitHeight(20);

        Icon icon = FileSystemView.getFileSystemView().getSystemIcon(file);
        int width = icon.getIconWidth();
        int height = icon.getIconHeight();

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        icon.paintIcon(null, graphics, 0, 0);
        graphics.dispose();
        WritableImage writableImage = new WritableImage(width, height);
        SwingFXUtils.toFXImage(bufferedImage, writableImage);
        imageView.setImage(writableImage);

        textLabel = new Label();

        // Create an HBox to hold the ImageView and Label
        hbox = new HBox(10);
        hbox.getChildren().addAll(imageView, textLabel);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            // Clear the cell if there is no item
            setGraphic(null);
        } else {
            // Set the text and image for the cell
            textLabel.setText(item);

            // Set the HBox as the graphic for the cell
            setGraphic(hbox);
        }
    }
}