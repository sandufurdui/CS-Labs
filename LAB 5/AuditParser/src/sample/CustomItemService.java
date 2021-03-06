package sample;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomItemService extends ViewController {
    private final List<String> updatedList = new ArrayList<>();
    private final JSONArray finalAudit;
    private JSONArray updateListOfItems = new JSONArray();
    private ListView<String> listView;
    private ObservableList<String> observableList;
    private Map<String, Object> objectMap = new HashMap<String, Object>();
    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final String pas = "1507";
    public TextField textListViewField;
    ArrayList<Integer> indexesArrayItem = new ArrayList<>();
    public List<Integer> selectedIndicesArray;


    public CustomItemService(JSONArray jsonArray, ListView<String> listView, ObservableList<String> observableList, TextField textField) {
        this.finalAudit = jsonArray;
        this.listView = listView;
        this.observableList = observableList;
        this.textListViewField = textField;
    }

    public void filter(ListView<String> listView) {
        FilteredList<String> filteredData = new FilteredList<>(observableList, s -> true);
        textListViewField.textProperty().addListener(obs -> {
            String filter = textListViewField.getText();
            if (filter == null || filter.length() == 0) {
                indexesArrayItem.clear();
                filteredData.setPredicate(s -> true);

                listView.setItems(observableList);
            } else {
                indexesArrayItem.clear();
                for (int index = 0; index < observableList.size() - 1; index++) {
                    if (observableList.get(index).contains(filter)) {
                        indexesArrayItem.add(index);
                    }
                }
//                System.out.println(indexesArrayItem);
                filteredData.setPredicate(s -> s.contains(filter));
                ObservableList<String> currentList = FXCollections.observableArrayList(filteredData);
                listView.setItems(currentList);
            }
        });
    }


    public void executeCommand() throws IOException, InterruptedException {
//        System.out.println(finalAudit);
        for (int i = 0; i < finalAudit.length(); i++) {
            objectMap = finalAudit.getJSONObject(i).toMap();
            if (objectMap.containsKey(" cmd ")) {
                matchOutputWithExpectedResult(objectMap);
            } else {
                updatedList.add("--Skipped--" + objectMap.get(" description ").toString());
            }
            updateListView(updatedList);
        }
    }

    public void updateListView(List<String> updatedList) {
        observableList = FXCollections.observableArrayList(updatedList);
        listView.setItems(observableList);
        textListViewField.clear();

        MultipleSelectionModel<String> langsSelectionModel = listView.getSelectionModel();

        langsSelectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        langsSelectionModel.getSelectedIndices().addListener((ListChangeListener<? super Number>) (observableValue) -> {
            selectedIndicesArray = (List<Integer>) observableValue.getList();
        });

        filter(listView);
        listView.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        if (item != null && item.contains("Success!")) {
                            setStyle("-fx-control-inner-background: derive(#98fb98, 50%);");
                        } else if (item != null && item.contains("-Skipped-")) {
                            setStyle("-fx-control-inner-background: derive(yellow, 50%);");
                        } else if (item != null && item.contains("Failure!")) {
                            setStyle("-fx-control-inner-background: derive(red, 100%);");
                        }
                    }
                };
            }
        });
    }


    public void enablePolicy() throws IOException, InterruptedException {
        Map object = new HashMap();
        StringBuilder output = new StringBuilder();
//        System.out.println("Selected indices:" + selectedIndicesArray);
        updatedList.clear();
        for (Integer index : selectedIndicesArray) {
            if (indexesArrayItem.size() > 0) {
                updateListOfItems.put(finalAudit.getJSONObject(indexesArrayItem.get(index)));
                object = finalAudit.getJSONObject(indexesArrayItem.get(index)).toMap();
            } else {
                updateListOfItems.put(finalAudit.getJSONObject(index));
                object = finalAudit.getJSONObject(index).toMap();
            }
            String description = (String) object.get(" description ");
            if (description.contains("Enable Gatekeeper")) {
                String command = "echo " + pas + " | sudo -S sudo spctl --master-disable";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Set time and date automatically")) {
                String command = "echo " + pas + " | sudo -S systemsetup -setusingnetworktime on";
                System.out.println(command);
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Bluetooth 'Discoverable'")) {
                String command = "echo " + pas + " | sudo -S blueutil -d 0";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Disable Remote Apple Events")) {
                String command = "echo " + pas + " | sudo systemsetup -setremoteappleevents off";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Enable Firewall Stealth Mode")) {
                String command = "echo " + pas + " | sudo -S sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setstealthmode on";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Wake for network access")) {
                String command = "echo " + pas + " | sudo -S pmset -a powernap 0";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Ensure Firewall is configured to log")) {
                String command = "echo " + pas + " | sudo -S /usr/libexec/ApplicationFirewall/socketfilterfw --setloggingmode on";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Disable 'Wake for network access' and 'Power Nap' - womp")) {
                String command = "echo " + pas + " | sudo -S pmset -a powernap 0";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else {
                updatedList.add("---Skipped---" + object.get(" description ").toString());
            }
        }
        updateListView(updatedList);
    }

    public void rollbackChanges() throws IOException, InterruptedException {
        Map object = new HashMap();
        StringBuilder output = new StringBuilder();
        System.out.println("Selected indices:" + selectedIndicesArray);
        updatedList.clear();
        for (Integer index : selectedIndicesArray) {
            if (indexesArrayItem.size() > 0) {
                object = updateListOfItems.getJSONObject(indexesArrayItem.get(index)).toMap();
            } else {
                object = updateListOfItems.getJSONObject(index).toMap();
            }
            String description = (String) object.get(" description ");
            if (description.contains("Enable Gatekeeper")) {
                String command = "echo " + pas + " | sudo -S spctl --master-disable";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Set time and date automatically")) {
                String command = "echo " + pas + " | sudo -S systemsetup -setusingnetworktime on";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Bluetooth 'Discoverable'")) {
                String command = "echo " + pas + " | sudo -S blueutil -d 0";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Disable Remote Apple Events")) {
                String command = "echo " + pas + " | sudo systemsetup -setremoteappleevents on";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Enable Firewall Stealth Mode")) {
                String command = "echo " + pas + " | sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setstealthmode off";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Wake for network access")) {
                String command = "echo " + pas + " | sudo -S pmset -a powernap 0";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Ensure Firewall is configured to log")) {
                String command = "echo " + pas + " | sudo  -S /usr/libexec/ApplicationFirewall/socketfilterfw --setloggingmode on";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else if (description.contains("Enable Firewall Stealth Mode")) {
                String command = "sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setstealthmode off";
                buildProcess(output, command);
                matchOutputWithExpectedResult(object);
            } else {
                updatedList.add("---Skipped---" + object.get(" description ").toString());
            }
            updateListView(updatedList);
        }
    }

    private void buildProcess(StringBuilder output, String command) throws IOException, InterruptedException {
        Process process = null;
        processBuilder.command("bash", "-c", "echo " + pas + " | sudo -S " + command.replaceAll("\"", ""));
        process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append("\n" + line);
        }
        System.out.println("Output of a command:" + output);
    }

    private void matchOutputWithExpectedResult(Map object) throws IOException, InterruptedException {
        String command = (String) object.get(" cmd ");
        command = command.replaceAll("\"", "");
        StringBuilder output = new StringBuilder();
        buildProcess(output, command);
        System.out.println(command);
        System.out.println(output);
        String expectedResult = object.get(" expect ").toString();
        if (command.contains("keychain")) {
            expectedResult = "Keychain[\\s]*\\\"\\<NULL\\>\\\"[\\s]*lock\\-on\\-sleep";
        } else {
            expectedResult = expectedResult.replaceAll("\\\\s", "\\s").replace("\"", "").replaceAll("  +", " ").trim();
        }
        Pattern pattern = Pattern.compile(expectedResult, Pattern.DOTALL);
        Matcher m = pattern.matcher(output.toString().replaceAll("\\n", "").replaceAll("\\t", "").replaceAll("\\r", ""));
        try {
            if (m.find()) {
                updatedList.add("Success! " + object.get(" description ").toString());
            } else {
                updatedList.add("Failure! " + object.get(" description ").toString());
            }
        } catch (Exception e) {
            System.out.println("Something went wrong");

        }
    }

}
