package sample;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ViewController implements Initializable {
    private Desktop desktop = Desktop.getDesktop();
    @FXML
    private static String finalJSON = "";
    @FXML
    private TextField urlTextField;
    @FXML
    private Button chooseAFile;
    private Future<List<String>> future;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TextFileReader reader = new TextFileReader();
    @FXML
    public  ListView<String> listView = new ListView<String>();
    private Set<String> stringSet;
    private List<String> selectedDescriptions = new ArrayList<>();
    @FXML
    JSONArray jsonArr = new JSONArray();
    CustomItemService customItemService;
    @FXML
    private TextField filterField;
    @FXML
    public TextField textListViewField;
    @FXML
    private TableView<ListViewCell> auditTable;
    @FXML
    private TableColumn<ListViewCell, String> NameColumn;
    public ObservableList<String> observableList = FXCollections.observableArrayList();
    private Window stage;
    private File file;
    public  List<Integer> selectedIndices;
    ArrayList<Integer> indexesArray = new ArrayList<>();
    public static JSONArray finalAudit = new JSONArray();


    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void chooseAFile(ActionEvent event) throws InterruptedException, ExecutionException {
        event.consume();
        FileChooser fileChooser = new FileChooser();
        chooseAFile.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(final ActionEvent e) {
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Audit", "*.audit"));
                File file = fileChooser.showOpenDialog(stage);
                if (file == null) try {
                    throw new FileNotFoundException();
                } catch (FileNotFoundException fileNotFoundException) {
                    fileNotFoundException.printStackTrace();
                }
                System.out.println("Chosen parsed file:" + file.getName());
                future = executorService.submit(new Callable<List<String>>() {
                    public List<String> call() throws Exception {
                        return Collections.singletonList(reader.read(file));
                    }
                });

                String textFuture = null;
                try {
                    textFuture = future.get().get(0);
                } catch (InterruptedException | ExecutionException interruptedException) {
                    interruptedException.printStackTrace();
                }

                Pattern pattern = Pattern.compile("<custom_item>(.*?)</custom_item>", Pattern.DOTALL);
                Matcher m = pattern.matcher(textFuture.replaceAll(" +", " ").trim());

                executorService.shutdownNow();
                String line = null;
                LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
                String tempKey = null;
                while (m.find()) {
                    String text = m.group(1).trim();
                    Reader inputString = new StringReader(text);
                    BufferedReader reader = new BufferedReader(inputString);
                    while (true) {
                        try {
                            if ((line = reader.readLine()) == null)
                                break;
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                        String[] parts = line.split(":", 2);
                        if (parts.length >= 2) {
                            String key = parts[0];
                            String value = parts[1];
                            map.put(key, value);
                            tempKey = key;
                        } else if (!line.contains(" : ")) {
                            map.put(tempKey, map.get(tempKey) + parts[0]);
                        }
                    }
                    JSONObject jsonObject = new JSONObject(map);
                    map.clear();
                    jsonArr.put(jsonObject);
                }
                displayDescriptions();

            }
        });

    }

    public void enableChosenPolicy(ActionEvent event) throws InterruptedException, IOException {
        event.consume();
        customItemService.enablePolicy();

    }

    public void rollbackChanges(ActionEvent event) throws IOException, InterruptedException {
        event.consume();
        customItemService.rollbackChanges();

    }

    public void saveChosenItems(ActionEvent event) throws IOException, InterruptedException {
        event.consume();
        if (indexesArray.size() > 0) {
            selectedIndices.forEach(index -> {
                finalAudit.put(jsonArr.get(indexesArray.get(index)));
            });
        } else {
            selectedIndices.forEach(index -> {
                finalAudit.put(jsonArr.get(index));
            });
        }
        Map<String, Object> map = new HashMap<String, Object>();
        String finalResult = "";
        for (int i = 0; i < finalAudit.length(); i++) {
            map = finalAudit.getJSONObject(i).toMap();
            finalResult += "<Custom item>\n" + map.entrySet().stream().map((entry) -> //stream each entry, map it to string value
                    entry.getKey() + ":" + entry.getValue() + "\n")
                    .collect(Collectors.joining(" ")) + "</Custom item>\n\n";
        }
        System.out.println(finalResult);
//        final Pattern compile = Pattern.compile("\\{(.*?)\\}");
//        Matcher match = compile.matcher(finalAudit.replaceAll("\\{\\", "<custom_item>)");
//        String[] objects =
        String pattern = "yyMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String filename = "ChosenAudits" + "(" + simpleDateFormat.format(new Date()) + ")" + ".audit";
        FileWriter file = new FileWriter(filename);
        file.write(String.valueOf(finalResult));
    customItemService = new CustomItemService(finalAudit, listView, observableList, textListViewField);
        customItemService.executeCommand();
        file.close();
    }

    public void setListView(List<String> list) {
        if (listView.getItems() != null)
            listView.getItems().clear();
        listView.refresh();

        observableList = FXCollections.observableArrayList(list);

        listView.setItems(observableList);

        MultipleSelectionModel<String> langsSelectionModel = listView.getSelectionModel();

        langsSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        langsSelectionModel.getSelectedIndices().addListener((ListChangeListener<? super Number>) (observableValue) -> {
            selectedIndices = (List<Integer>) observableValue.getList();
        });
        filter(listView);
    }

    public void filter(ListView<String> listView){
        FilteredList<String> filteredData = new FilteredList<>(observableList, s -> true);
        textListViewField.textProperty().addListener(obs -> {
            String filter = textListViewField.getText();
            if (filter == null || filter.length() == 0) {
                indexesArray.clear();
                filteredData.setPredicate(s -> true);

                listView.setItems(observableList);
            } else {
                indexesArray.clear();
                for (int index = 0; index < observableList.size() -1; index++) {
                    if (observableList.get(index).contains(filter)) {
                        indexesArray.add(index);
                    }
                }
                filteredData.setPredicate(s -> s.contains(filter));
                ObservableList<String> currentList = FXCollections.observableArrayList(filteredData);
                listView.setItems(currentList);
            }
        });
    }

    public void displayDescriptions() {

        List<String> descriptionsList = new ArrayList<>();
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObject = jsonArr.getJSONObject(i);
            descriptionsList.add(jsonObject.get(" description ").toString());
        }

        System.out.println("\nAll descriptions:\n  ");
        descriptionsList.forEach(description -> System.out.println(description + "\n"));
        setListView(descriptionsList);
    }

    public class TextFileReader {
        public String read(File file) throws FileNotFoundException {
            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}

