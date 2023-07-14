package com.fileup;

import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.observableArrayList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class HelloController {

    private ObservableList<String> filesListView;
    private static File currPath;

    @FXML
    private ChoiceBox<String> drivesChoice;
    @FXML
    private ListView<String> directoriesAndFiles;
    @FXML
    private TextField fileExtensionText;
    @FXML
    private Text filesSearchedText, filesSearchedPerSecondText, timeElapsedText;
    @FXML
    private Button goBackBtn;

    @FXML
    private TextField searchField;

    public void initialize() {
        var drives = getDrives();
        filesListView = observableArrayList();

        for (File file : drives) {
            drivesChoice.getItems().add(file.toString());
            drivesChoice.getSelectionModel().selectFirst();
            addItemToList(String.valueOf(file));
            directoriesAndFiles.setCellFactory(param -> new ImageTextListCell(file));
            directoriesAndFiles.getItems().add(file.toString());
        }
        drivesChoice.getItems().add("ALL");

        searchField.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                System.out.println(searchField.getText());
            }
        });
    }

    @FXML
    private void filesListViewClicked(MouseEvent event) {
        List<File> files = null;
        if (event.getClickCount() == 2) {
            String selectedItem = directoriesAndFiles.getSelectionModel().getSelectedItem();

            try {
                files = listFiles(selectedItem);
                if (files != null) {
                    directoriesAndFiles.getItems().clear();
                    currPath = files.get(0).getParentFile();
                    for (File file : files) {
                        if (file.isFile()) {
                            if (!fileExtensionText.getText().isEmpty()) {
                                if (file.getName().toLowerCase().contains(fileExtensionText.getText().toString().toLowerCase())) {
                                    directoriesAndFiles.setCellFactory(param -> new ImageTextListCell(file));
                                }
                            } else {
                                directoriesAndFiles.setCellFactory(param -> new ImageTextListCell(file));
                            }
                        } else {
                            directoriesAndFiles.setCellFactory(param -> new ImageTextListCell(file));
                        }
                        directoriesAndFiles.getItems().add(file.toString());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void search() {
        var fileName = searchField.getText().toString();
        var drive = drivesChoice.getSelectionModel().getSelectedItem();
        var fileExtension = fileExtensionText.getText().toString();
        directoriesAndFiles.getItems().clear();

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
                timeline.setCycleCount(Animation.INDEFINITE);
                timeline.play();

                while (!stack.isEmpty() && !isCancelled()) {
                    File currentDir = stack.pop();
                    File[] files = currentDir.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            filesSearched.incrementAndGet();
                            if (file.isDirectory()) {
                                stack.push(file);
                            } else {
                                if (file.getName().toLowerCase().contains(targetFileName) && file.getName().toLowerCase().endsWith(targetFileExtension.toLowerCase())) {
                                    Platform.runLater(() -> {
                                        System.out.println("Found file: " + file.getAbsolutePath());
                                        populateFileTable(file);
                                        foundFiles.add(file);
                                    });
                                }
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
            directoriesAndFiles.setCellFactory(param -> new ImageTextListCell(file));
            directoriesAndFiles.getItems().add(file.getAbsolutePath());
        });
    }

    @FXML
    private void goBack() {
        directoriesAndFiles.getItems().clear();
        var parent = currPath.getParentFile();
        for (File file : parent.listFiles()) {
            directoriesAndFiles.getItems().add(file.toString());
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