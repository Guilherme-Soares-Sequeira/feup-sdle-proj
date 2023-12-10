package org.C2.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.*;
import org.C2.cloud.LoadBalancer;
import org.C2.cloud.database.KVStore;
import org.C2.crdts.ORMap;
import org.C2.utils.*;
import org.eclipse.jetty.server.Server;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLOutput;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockUI extends JFrame {
    private final KVStore kvstore;
    private final int width;
    private final int height;
    private String url;
    private String forID;
    private ORMap sl;
    private JPanel addItemPanel;
    private JPanel itemListPanel;

    // TODO 1: add a 'SAVE' button which stores the current version of the shopping list locally (maybe done)
    // TODO 2: add a 'CREATE LIST' button which instantiates a new shopping list (maybe done)
    // TODO 3: add a log message for when the polling for a PULL request is happening (maybe done)
    // TODO 4: add a log message for errors (PUSH/PULL fails) (maybe done)

    public MockUI() {
        this.kvstore = new KVStore("users", true);

        this.loadUI();

        this.width = 1280;
        this.height = 720;

        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setTitle("Shopping List");
        super.setSize(1500, 1000);
        super.setLocationRelativeTo(null);
        super.setVisible(true);
    }

    private void loadUI() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter shopping list URL: ");
        JButton button = new JButton("Submit");
        JButton createShoppingListButton = new JButton("New Shopping List");

        JTextField urlTextField = new JTextField(36);

        this.itemListPanel = new JPanel();
        this.itemListPanel.setLayout(new BoxLayout(this.itemListPanel, BoxLayout.Y_AXIS));

        button.addActionListener(e -> {
            String url = urlTextField.getText();
            this.url = url;

            Optional<String> list = this.kvstore.get(url);

            if (list.isPresent()) {
                try {
                    this.showShoppingList(list.get());
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                super.dispose();
            } else {
                System.err.println("That shopping list does not exist.");
            }

        });

        createShoppingListButton.addActionListener(e -> {
            this.url = UUID.randomUUID().toString();

            ORMap empty = new ORMap(this.url);

            String emptyList = empty.toJson();

            try {
                showShoppingList(emptyList);
                
                super.dispose();
            } catch (JsonProcessingException ex) {
                System.err.println("Could not display the created shopping list: " + ex);
            }
        });

        panel.add(label);
        panel.add(urlTextField);
        panel.add(button);
        panel.add(createShoppingListButton);

        super.add(panel);
    }

    private void showShoppingList(String list) throws JsonProcessingException {
        this.sl = ORMap.fromJson(list);

        this.updateItemList();

        JFrame frame = new JFrame(this.url);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(this.width, this.height);
        frame.setLocationRelativeTo(this);

        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout());
        container.add(this.createAddItemPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(this.itemListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.addItemPanel.add(new JLabel("URL = " + this.url));

        container.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JPanel createAddItemPanel() {
        this.addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addItemPanel.setBackground(Color.LIGHT_GRAY);

        JTextField itemNameField = new JTextField(36);
        itemNameField.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel itemNameLabel = new JLabel("Item name: ");
        itemNameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton addItemButton = this.createButton("ADD", Font.MONOSPACED);
        JButton pullButton = this.createButton("PULL", Font.MONOSPACED);
        JButton pushButton = this.createButton("PUSH", Font.MONOSPACED);
        JButton saveButton = this.createButton("SAVE", Font.MONOSPACED);

        addItemButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_ADD_PATH)));
        pullButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_PULL_PATH)));
        pushButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_PUSH_PATH)));

        addItemButton.addActionListener(e -> this.addItem(itemNameField));
        pushButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String endpoint = "http://localhost:2000/write/" + url;

                performWriteRequest(endpoint);
            }
        });

        pullButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pull button was called.");
                if (!performReadRequest()) {
                    System.out.println("performReadRequest() failed");
                    return;
                }

                CompletableFuture<Optional<RequestStatus>> future = CompletableFuture.supplyAsync(() -> performPollRequest());

                Optional<RequestStatus> pollStatus;
                try {
                    System.out.println("Im in the poll status");
                    pollStatus = future.get(4000, TimeUnit.MILLISECONDS);
                } catch ( InterruptedException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
                    System.out.println("Catched exception: " + ex);
                    future.cancel(true);
                    return;
                }

                System.out.println("Hello 1");

                if (pollStatus.isEmpty() || !pollStatus.get().equals(RequestStatus.DONE)) {
                    System.out.println("Displaying response time out message");
                    displayTemporaryMessage(addItemPanel, "Response timeout", 3);

                    return;
                }

                System.out.println("Hello 2");

                Optional<ORMap> fetchedList = performFetchReadDataRequest();

                if (fetchedList.isPresent()) {
                    System.out.println("I got a list from the read request");

                    for (Pair<String, Integer> entry: fetchedList.get().read()) {
                        String k = entry.getFirst();
                        int v = entry.getSecond();

                        System.out.println("k: " + k);
                        System.out.println("v: " + v);
                    }

                    sl.join(fetchedList.get());

                    System.out.println("After joining the received CRDT:");

                    for (Pair<String, Integer> entry: sl.read()) {
                        String k = entry.getFirst();
                        int v = entry.getSecond();

                        System.out.println("merged k: " + k);
                        System.out.println("merged v: " + v);
                    }

                    updateItemList();
                }

            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                kvstore.put(url, sl.toJson());
            }
        });

        addItemPanel.add(itemNameLabel);
        addItemPanel.add(itemNameField);
        addItemPanel.add(addItemButton);
        addItemPanel.add(pullButton);
        addItemPanel.add(pushButton);
        addItemPanel.add(saveButton);

        return addItemPanel;
    }

    private void performWriteRequest(String endpoint) {
        System.out.println("[UI] - You have clicked the PUSH button");

        for (Pair<String, Integer> entry: this.sl.read()) {
            String k = entry.getFirst();
            int v = entry.getSecond();

            System.out.println("[UI] - item: " + k + " | quantity: " + v);
        }

        HttpResult<String> result = ServerRequests.requestWrite(LoadBalancer.lbinfo, url, this.sl);

        if (!result.isOk()) {
            displayTemporaryMessage(this.addItemPanel, "Write timeout", 3);
            System.err.println(result.errorMessage());
        } else {
            this.forID = result.get();
        }
    }

    private boolean performReadRequest() {
        HttpResult<String> result = ServerRequests.requestRead(LoadBalancer.lbinfo, this.url);

        this.forID = result.get();

        return result.isOk();
    }

    private Optional<RequestStatus> performPollRequest() {
        while (!Thread.currentThread().isInterrupted()) {
            HttpResult<RequestStatus> result = ServerRequests.pollRequest(LoadBalancer.lbinfo, this.forID);

            if (!result.isOk()) {
                System.err.println("Error when performing poll request: " + result.errorMessage());
                try {
                    Thread.sleep(250);
                } catch (Exception e) {
                    // do nothing
                }
                continue;
            }  else if (result.get().equals(RequestStatus.PROCESSING)) {
                System.err.println("Polling not ready yet.");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {

                }
                continue;
            }

            System.out.println("Finished polling! result = " + result.get().toString());
            return Optional.of(result.get());
        }

        throw new RuntimeException("Finished unsuccessfully");
    }

    private Optional<ORMap> performFetchReadDataRequest() {
        HttpResult<FetchListInfo> result = ServerRequests.fetchList(LoadBalancer.lbinfo, this.forID);

        if (!result.isOk()) {
            System.err.println("Error fetching list from load balancer:" + result.errorMessage());
            return Optional.empty();
        } else if (result.get().getStatus() != RequestStatus.DONE) {
            System.err.println("Fetch status wasn't ready.");
            return Optional.empty();
        }

        FetchListInfo info = result.get();

        return Optional.of(info.getListJson());
    }

    private void addItem(JTextField field) {
        String name = field.getText();

        if (!name.isEmpty()) {
            this.sl.insert(name);
            this.updateItemList();
            field.setText("");
        }
    }

    private void updateItemList() {
        this.itemListPanel.removeAll();

        for (Pair<String, Integer> entry: this.sl.read()) {
            JPanel itemContainer = new JPanel(new BorderLayout());

            String item = entry.getFirst();
            int quantity = entry.getSecond();

            JLabel nameLabel = new JLabel(item);
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leftPanel.add(nameLabel);
            itemContainer.add(leftPanel, BorderLayout.WEST);

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JLabel quantityLabel = new JLabel("Quantity: " + quantity);
            JButton incrementButton = this.createButton("+", UIConstants.FONT_MONOSPACED);
            JButton decrementButton = this.createButton("-", UIConstants.FONT_MONOSPACED);

            incrementButton.addActionListener(createIncrementActionListener(item));
            decrementButton.addActionListener(createDecrementActionListener(item, quantity));

            rightPanel.add(quantityLabel);
            rightPanel.add(incrementButton);
            rightPanel.add(decrementButton);
            itemContainer.add(rightPanel, BorderLayout.EAST);

            itemContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            itemListPanel.add(itemContainer);
        }

        this.itemListPanel.revalidate();
        this.itemListPanel.repaint();
    }

    private ActionListener createIncrementActionListener(String itemName) {
        return e -> {
            Optional<Integer> currentQuantity = this.sl.get(itemName);

            currentQuantity.ifPresent(integer -> this.sl.put(itemName, integer + 1));

            updateItemList();
        };
    }

    private ActionListener createDecrementActionListener(String itemName, int currentQuantity) {
        return e -> {
            this.sl.put(itemName, currentQuantity - 1);
            updateItemList();
        };
    }

    private JButton createButton(String text, String font) {
        JButton button = new JButton(text);

        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.setFont(new Font(font, Font.BOLD, 14));
        button.setFocusPainted(false);

        return button;
    }

    private void displayTemporaryMessage(JPanel panel, String message, int duration) {
        JLabel label = new JLabel(message);

        panel.add(label);

        Timer timer = new Timer(duration, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.remove(label);
            }
        });

        timer.setRepeats(false);
        timer.start();
    }

    private URL loadIcon(String path) {
        return Objects.requireNonNull(getClass().getResource(path));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MockUI::new);
    }
}
