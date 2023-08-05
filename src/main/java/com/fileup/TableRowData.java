package com.fileup;

import java.util.Objects;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.ImageView;

public class TableRowData {

    private ImageView image;
    private SimpleStringProperty name;
    private SimpleStringProperty path;

    public TableRowData(ImageView image, String label1, String label2) {
        this.image = new ImageView(image.getImage());
        this.name = new SimpleStringProperty(label1);
        this.path = new SimpleStringProperty(label2);
    }

    public ImageView getImage() {
        return image;
    }

    public String getName() {
        return name.get();
    }

    public String getPath() {
        return path.get();
    }
}