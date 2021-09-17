package sample;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        getStageData(stage);
    }


    public static void main(String[] args) {
        launch(args);
    }

    private List<String> listFilesForFolder(final File folder) {
        List<String> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                files.add(fileEntry.getName());
            }
        }
        return files;
    }

    private String showPolicyDetails(URL path) {
        try {
            File file = new File(path.getPath());
            Scanner sc = new Scanner(file);
            sc.useDelimiter("\\Z");
            return sc.next().replace("\n", "<$replace_later>").replace("\r", "<$replace_later>");
        } catch (FileNotFoundException ex) {
            System.out.println(ex);
            return "";
        }
    }

    private String parsePolicyInfo(String info) {
        String response = "";
        List<String> emptyList = new ArrayList<>();

        List<String> metadata = getSubstringBetweenElements("<ui_metadata>", "</ui_metadata>", info);
        List<String> displayName = getSubstringBetweenElements("<display_name>", "</display_name>", metadata.get(0));
        response += returnStringFromArrayItems("Display name", displayName, "");
        List<String> typeSpec = getSubstringBetweenElements("<type>", "</type>", metadata.get(0));
        response += returnStringFromArrayItems("Type spec", typeSpec, "");
        List<String> name = getSubstringBetweenElements("<name>", "</name>", metadata.get(0));
        response += returnStringFromArrayItems("Name", name, "");
        List<String> version = getSubstringBetweenElements("<version>", "</version>", metadata.get(0));
        response += returnStringFromArrayItems("Version", version, "");
        List<String> link = getSubstringBetweenElements("<link>", "</link>", metadata.get(0));
        response += returnStringFromArrayItems("Link", link, "");
        List<String> labels = getSubstringBetweenElements("<labels>", "</labels>", metadata.get(0));
        response += returnStringFromArrayItems("Labels", labels, "");
        List<String> benchMark = getSubstringBetweenElements("<benchmark_refs>", "</benchmark_refs>", metadata.get(0));
        response += returnStringFromArrayItems("Benchmark refs", benchMark, "");

        List<String> variables = getSubstringBetweenElements("<variables>", "</variables>", metadata.get(0));
        response += returnStringFromArrayItems("Variables", emptyList, "\n\n\n");

        for (int i = 0; i < variables.size(); i++) {
            response += returnStringFromArrayItems("Name",
                    getSubstringBetweenElements("<name>", "</name>", variables.get(i)), "");
            response += returnStringFromArrayItems("Default",
                    getSubstringBetweenElements("<default>", "</default>", variables.get(i)), "");
            response += returnStringFromArrayItems("Description",
                    getSubstringBetweenElements("<description>", "</description>", variables.get(i)), "");
            response += returnStringFromArrayItems("Info",
                    getSubstringBetweenElements("<info>", "</info>", variables.get(i)), "");
        }
        response += returnStringFromArrayItems("", emptyList, "\n\n\n");

        List<String> type = getSubstringBetweenElements("<check_type:", ">", info);
        response += returnStringFromArrayItems("Windows version", type, "");
        List<String> groupPolicy = getSubstringBetweenElements("<group_policy:", ">", info);
        response += returnStringFromArrayItems("Group policy", groupPolicy, "");
        List<String> customItems = getSubstringBetweenElements("<custom_item>", "</custom_item>", info);
        response += returnStringFromArrayItems("Custom items", customItems, "\n\n\n");
        return response;
    }

    private List<String> getSubstringBetweenElements(String el1, String el2, String el) {
        List<String> rows = new ArrayList();

        String strMatch = "(?<=" + el1 + ")(.*?)(?=" + el2 + ")";
        Matcher matcher = Pattern.compile(strMatch).matcher(el);
        while (matcher.find()) {
            rows.add(matcher.group());
        }
        return rows;
    }

    private String returnStringFromArrayItems(String title, List<String> items, String stringToAdd) {
        String returnString = title + ": ";
        for (int i = 0; i < items.size(); i++) {
            returnString += items.get(i) + "\n\n" + stringToAdd;
        }
        returnString = returnString.replace("<$replace_later>", "\n");
        return returnString;
    }

    private void saveFileCopy(String path, Stage stage) {
        try {
            String fileInitialPath = path;
            path = getSubstringBetweenElements("policies/", ".audit", path).get(0);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            LocalDateTime now = LocalDateTime.now();
            String newFileName = "src/policies/" + path + "_copy_" + dtf.format(now) + ".audit";
            File myObj = new File(newFileName);
            if (myObj.createNewFile()) {
                writeToFile(newFileName, fileInitialPath, stage);
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void writeToFile(String value, String path, Stage stage) {
        try {
            File file = new File(path);
            Scanner sc = new Scanner(file);
            sc.useDelimiter("\\Z");
            String fileData = sc.next();
            FileWriter myWriter = new FileWriter(value);
            myWriter.write(fileData);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
            restartApp(stage);
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void restartApp(Stage stage) {
        getStageData(stage);
    }

    private void getStageData(Stage stage) {
        ScrollPane scrollPane = new ScrollPane();

        stage.setTitle("Windows Security");
        final Group rootGroup = new Group();

        URL folderUrl = getClass().getResource("/policies");
        final File folder = new File(folderUrl.getPath());
        List<String> files = listFilesForFolder(folder);
        Text policyContent = new Text(870, (40) + 15, "No selected policy.");

        // get all content for each policy
        for (int i = 0; i < files.size(); i++) {
            URL policyUrl = getClass().getResource("/policies/" + files.get(i));

            Button buttonShowMore = new Button("Show Details");
            buttonShowMore.setLayoutX(25);
            buttonShowMore.setLayoutY(((i + 1) * 40) - 5);
            buttonShowMore.setOnAction(value -> {
                policyContent.setText(parsePolicyInfo(showPolicyDetails(policyUrl)));
            });
            rootGroup.getChildren().add(buttonShowMore);

            Button buttonCopy = new Button("Save copy");
            buttonCopy.setLayoutX(130);
            buttonCopy.setLayoutY(((i + 1) * 40) - 5);
            buttonCopy.setOnAction(value -> {
                saveFileCopy(policyUrl.getPath(), stage);
            });
            rootGroup.getChildren().add(buttonCopy);

            final Text policyFile = new Text(220, ((i + 1) * 40) + 15, files.get(i));
            policyFile.setFill(Color.CHOCOLATE);
            policyFile.setFont(Font.font(java.awt.Font.SERIF, 18));
            policyFile.setWrappingWidth(700);
            rootGroup.getChildren().add(policyFile);
        }
        policyContent.setFill(Color.BLACK);
        policyContent.setFont(Font.font(java.awt.Font.SERIF, 12));
        policyContent.setWrappingWidth(500);
        rootGroup.getChildren().add(policyContent);

        // Set content for ScrollPane
        scrollPane.setContent(rootGroup);

        // Always show vertical scroll bar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        final Scene scene =
                new Scene(scrollPane, 1440, 700, Color.BEIGE);
        stage.setScene(scene);
        stage.show();
    }
}


