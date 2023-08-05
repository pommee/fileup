package com.fileup;

import static java.util.stream.Collectors.toList;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.collections.FXCollections.observableArrayList;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.MouseButton.SECONDARY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class Controller {

    private ObservableList<String> filesListView;
    private static File currPath;
    private static List<FileSystemItem> cachedItems;

    @FXML
    private ChoiceBox<String> drivesChoice;
    @FXML
    private TableView<TableRowData> directoriesAndFiles;
    @FXML
    private TextField fileExtensionText;
    @FXML
    private Text filesSearchedText, filesSearchedPerSecondText, timeElapsedText;
    @FXML
    private Button goBackBtn;

    @FXML
    private TextField searchField;

    public void initialize() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("cache.dat"))) {
            long startTime = System.currentTimeMillis();
            cachedItems = (List<FileSystemItem>) ois.readObject();
            long endTime = System.currentTimeMillis();
            System.out.println("Took " + (endTime - startTime) + " ms to get cache");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Cache not done previously, searching and creating a new .dat");
        }

        var drives = getDrives();
        filesListView = observableArrayList();

        drivesChoice.getItems().addAll(drives.stream().map(File::toString).toList());
        drivesChoice.getItems().add("ALL");
        drivesChoice.getSelectionModel().selectFirst();

        searchField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(ENTER)) {
                System.out.println(searchField.getText());
            }
        });

        TableColumn<TableRowData, ImageView> imageColumn = new TableColumn<>("Image");
        TableColumn<TableRowData, String> nameColumn = new TableColumn<>("Name");
        TableColumn<TableRowData, String> pathColumn = new TableColumn<>("Path");

        imageColumn.setCellValueFactory(new PropertyValueFactory<>("image"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));

        imageColumn.prefWidthProperty().bind(directoriesAndFiles.widthProperty().multiply(0.15));
        nameColumn.prefWidthProperty().bind(directoriesAndFiles.widthProperty().multiply(0.42));
        pathColumn.prefWidthProperty().bind(directoriesAndFiles.widthProperty().multiply(0.43));

        for (File file : drives) {
            addItemToList(String.valueOf(file));
            ImageView image = Helper.findImage(file);
            directoriesAndFiles.getItems().add(new TableRowData(image, file.getName(), file.getAbsolutePath()));
        }

        directoriesAndFiles.getColumns().addAll(imageColumn, nameColumn, pathColumn);
    }

    @FXML
    private void filesListViewClicked(MouseEvent event) {
        List<File> files;
        TableRowData selectedItem = directoriesAndFiles.getSelectionModel().getSelectedItem();

        if (event.getButton() == SECONDARY) {
            addPopupMenu(selectedItem, event);
        }
        if (event.getClickCount() == 2) {
            try {
                files = listFiles(selectedItem.getPath());
                if (files != null) {
                    directoriesAndFiles.getItems().clear();
                    currPath = files.get(0).getParentFile();
                    for (File file : files) {
                        if (file.isFile() && (fileExtensionText.getText().isEmpty() || file.getName().toLowerCase().contains(fileExtensionText.getText().toLowerCase()))) {
                            directoriesAndFiles.getItems().add(new TableRowData(null, file.getName(), file.getAbsolutePath()));
                        } else {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addPopupMenu(TableRowData path, MouseEvent event) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem menuItem1 = new MenuItem("Open in File Explorer");
        menuItem1.setOnAction(popupMenuEvent -> {
            try {
                Runtime.getRuntime().exec("explorer " + new File(path.getPath()).getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        contextMenu.getItems().addAll(menuItem1);
        directoriesAndFiles.setOnContextMenuRequested(eavent -> {
            contextMenu.show(directoriesAndFiles, event.getScreenX(), event.getScreenY());
        });
    }

    @FXML
    private void search() {
        var fileName = searchField.getText().toString();
        var drive = drivesChoice.getSelectionModel().getSelectedItem();
        var fileExtension = fileExtensionText.getText().toString();
        directoriesAndFiles.getItems().clear();

        if (cachedItems != null) {
            for (FileSystemItem item : cachedItems) {
                if (item.getPath().contains(drive)) {
                    if (item.getPath().toLowerCase().contains(fileName)) {
                        if (item.getPath().toLowerCase().endsWith(fileExtension)) {
                            System.out.println("Found: " + item.getPath());
                            populateFileTable(new File(item.getPath()));
                        }
                    }
                }
            }
        }

        if (drive.equals("ALL")) {
            searchAll(fileName, fileExtension);
        } else {
            new Thread(() -> searchFilesAndUpdateUI(new File(drive), fileName, fileExtension)).start();
        }
    }

    private void searchAll(String fileName, String fileExtension) {
        for (String file : drivesChoice.getItems()) {
            if (!file.equals("ALL")) {
                File driveToSearch = new File(file);
                new Thread(() -> searchFilesAndUpdateUI(driveToSearch, fileName, fileExtension)).start();
            }
        }
    }

    public void searchFilesAndUpdateUI(File directory, String targetFileName, String targetFileExtension) {
        List<FileSystemItem> cachedItems = new ArrayList<>();

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        AtomicLong filesSearched = new AtomicLong(0);

        Task<List<File>> task = new Task<>() {
            @Override
            protected List<File> call() {
                List<File> foundFiles = new ArrayList<>();
                Deque<File> stack = new ArrayDeque<>();
                stack.push(directory);

                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTimeSeconds = (currentTime - startTime.get()) / 1000;
                    long numFilesSearched = filesSearched.get();
                    int traversedPerSecond = (int) (numFilesSearched / elapsedTimeSeconds);

                    Platform.runLater(() -> {
                        updateTimeLabel(elapsedTimeSeconds);
                        updateTraversedPerSecondLabel(traversedPerSecond);
                        updateFilesSearched((int) filesSearched.get());
                    });
                }));
                timeline.setCycleCount(INDEFINITE);
                timeline.play();

                while (!stack.isEmpty() && !isCancelled()) {
                    File currentDir = stack.pop();
                    File[] files = currentDir.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            FileSystemItem item = new FileSystemItem(file.getAbsolutePath(), file.isDirectory());
                            if (!cachedItems.contains(item)) {
                                cachedItems.add(item);
                                filesSearched.incrementAndGet();
                            }

                            if (file.isDirectory()) {
                                stack.push(file);
                            } else if (
                                    targetFileExtension.isBlank() ||
                                            (
                                                    file.toString().contains(directory.toString()) &&
                                                    file.getName().toLowerCase().contains(targetFileName) &&
                                                    file.getName().toLowerCase().endsWith(targetFileExtension.toLowerCase()))
                            ) {
                                Platform.runLater(() -> {
                                    System.out.println("Found file: " + file.getAbsolutePath());
                                    populateFileTable(file);
                                    foundFiles.add(file);
                                });
                            }
                        }
                    }
                }

                timeline.stop();

                long elapsedTimeSeconds = (System.currentTimeMillis() - startTime.get()) / 1000;
                long numFilesSearched = filesSearched.get();
                int traversedPerSecond = (int) (numFilesSearched / elapsedTimeSeconds);

                Platform.runLater(() -> {
                    updateTimeLabel(elapsedTimeSeconds);
                    updateTraversedPerSecondLabel(traversedPerSecond);
                    updateFilesSearched((int) filesSearched.get());
                });

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("cache.dat"))) {
                    oos.writeObject(cachedItems);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return foundFiles;
            }
        };

        Thread thread = new Thread(task);
        thread.start();
    }

    private void updateTraversedPerSecondLabel(int traversedPerSecond) {
        if (traversedPerSecond >= 1_000_000) {
            filesSearchedPerSecondText.setText(String.format("%.2fM", traversedPerSecond / 1_000_000.0));
        } else if (traversedPerSecond >= 1_000) {
            filesSearchedPerSecondText.setText(String.format("%.2fK", traversedPerSecond / 1_000.0));
        } else {
            filesSearchedPerSecondText.setText(String.valueOf(traversedPerSecond));
        }
    }

    private void updateFilesSearched(int filesSearched) {
        if (filesSearched >= 1_000_000) {
            filesSearchedText.setText(String.format("%.2fM", filesSearched / 1_000_000.0));
        } else if (filesSearched >= 1_000) {
            filesSearchedText.setText(String.format("%.2fK", filesSearched / 1_000.0));
        } else {
            filesSearchedText.setText(String.valueOf(filesSearched));
        }
    }

    private void updateTimeLabel(long elapsedTimeSeconds) {
        String formattedTime = formatTime(elapsedTimeSeconds);
        timeElapsedText.setText(formattedTime);
    }

    private String formatTime(long elapsedTimeSeconds) {
        long minutes = elapsedTimeSeconds / 60;
        long seconds = elapsedTimeSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    private void populateFileTable(File file) {
        Platform.runLater(() -> {
            ImageView image = Helper.findImage(file);
            TableRowData rowData = new TableRowData(image, file.getName(), file.getAbsolutePath());
            if (!directoriesAndFiles.getItems().contains(rowData)) {
                directoriesAndFiles.getItems().add(rowData);
            }
        });
    }

    @FXML
    private void goBack() {
        directoriesAndFiles.getItems().clear();
        var parent = currPath.getParentFile();
        for (File file : parent.listFiles()) {
            directoriesAndFiles.getItems().add(new TableRowData(null, file.getName(), file.getAbsolutePath()));
        }
    }

    private static List<File> getDrives() {
        File[] drives = File.listRoots();
        if (drives != null && drives.length > 0) {
            return Arrays.stream(drives).collect(toList());
        }
        return null;
    }

    private void addItemToList(String drive) {
        filesListView.add(drive);
    }

    public List<File> listFiles(String dir) throws IOException, InterruptedException {
        File directoryPath = new File(dir);

        if (directoryPath.isFile()) {
            String[] commands = {
                    "cmd.exe",
                    "/c",
                    "start",
                    "\"DummyTitle\"",
                    "\"" + directoryPath.getAbsolutePath() + "\""
            };

            Process p = Runtime.getRuntime().exec(commands);
            p.waitFor();
            return null;
        }
        if (directoryPath.isDirectory()) {
            try {
                return List.of(directoryPath.listFiles());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("That dir is empty!");
                return null;
            }
        }
        return null;
    }

}