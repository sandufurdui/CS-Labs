package sample;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ViewController implements Initializable {
    private static final Logger LOG = Logger.getLogger(ViewController.class.getName());
    @FXML
    private static JSONArray jsonArr = new JSONArray();
    @FXML
    Label selectedLbl = new Label();
 
    @FXML
    private TextField urlTextField;
    @FXML
    private TextField textListViewField;
    @FXML
    private Button ConvertAndSave;
    private Future<List<String>> future;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private TextFileReader reader = new TextFileReader();
    @FXML
    private ListView<String> listView = new ListView<String>();
    private Set<String> stringSet;
    private List<Integer> selectedIndices;
    ArrayList<Integer> indexesArray = new ArrayList<>();
    private String initialFileName;


    public void setListView(List<String> list) {
        ObservableList<String> observableList = FXCollections.observableArrayList(list);

        listView.setItems(observableList);

        MultipleSelectionModel<String> langsSelectionModel = listView.getSelectionModel();

        langsSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        langsSelectionModel.getSelectedIndices().addListener((ListChangeListener<? super Number>) (observableValue) -> {
            selectedIndices = (List<Integer>) observableValue.getList();
        });

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

    public void initialize(URL url, ResourceBundle rb) {
    }

    public void saveFile() {
        List<String> descriptionsList = new ArrayList<>();
        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObject = jsonArr.getJSONObject(i);
            descriptionsList.add(jsonObject.get(" description ").toString());
        }

        setListView(descriptionsList);
    }

    public void exportItems(ActionEvent event) throws IOException {
        event.consume();
        JSONArray finalAudit = new JSONArray();
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
            finalResult += "<Custom item>\n" +  map.entrySet().stream().map((entry) -> 
                    entry.getKey() + ":" + entry.getValue() + "\n")
                    .collect(Collectors.joining(" ")) + "</Custom item>\n\n";
        }
        String pattern = "yyMMddHHmmssZ";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String filename = initialFileName.substring(0, initialFileName.length()-6) + "_" + simpleDateFormat.format(new Date()) + "_copy.audit";
        FileWriter file = new FileWriter(filename);
        file.write(String.valueOf(finalResult));
        file.close();
    }


    @FXML
    public void readAndConvertFile() throws InterruptedException, ExecutionException, IOException {
        File chosedFile = PickAFile();
        initialFileName = chosedFile.getName();
        future = executorService.submit(new Callable<List<String>>() {
            public List<String> call() throws Exception {
                return Collections.singletonList(reader.read(new File(chosedFile.getPath())));
            }
        });

        String textFuture = future.get().get(0);

        Pattern pattern = Pattern.compile("<custom_item>(.*?)</custom_item>", Pattern.DOTALL);
        Matcher m = pattern.matcher(textFuture.replaceAll(" +", " ").trim());

        executorService.shutdownNow();
        String line;
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        String tempKey = null;
        while (m.find()) {
            String text = m.group(1).trim();
            Reader inputString = new StringReader(text);
            BufferedReader reader = new BufferedReader(inputString);
            while ((line = reader.readLine()) != null) {
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
                jsonArr.put(jsonObject);
            }

        saveFile();
    }

    public File PickAFile() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                ".audit files", "audit");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }
}

