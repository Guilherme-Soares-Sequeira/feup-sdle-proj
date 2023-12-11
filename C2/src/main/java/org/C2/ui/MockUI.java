package org.C2.ui;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.*;
import org.C2.cloud.LoadBalancer;
import org.C2.cloud.database.KVStore;
import org.C2.crdts.ORMap;
import org.C2.utils.*;
import org.eclipse.jetty.server.Server;
import org.intellij.lang.annotations.Flow;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockUI extends JFrame {
    private KVStore kvstore;
    private final int width;
    private final int height;
    private String url;
    private String forID;
    private String username;
    private ORMap sl;
    private JPanel addItemPanel;
    private JPanel itemListPanel;

    public MockUI() {

        this.loadUI();

        this.width = 1280;
        this.height = 720;

        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setTitle("C2 - Cloud Cart");
        super.setSize(700, 400);
        super.setLocationRelativeTo(null);
        super.setVisible(true);
    }

    // insert username
    // insert url
    private void loadUI() {
        JPanel enterUsernamePanel = new JPanel();
        JLabel enterUsernameLabel = new JLabel("Enter username: ");
        JButton submitUsername = new JButton("OK");
        JTextField usernameTextField = new JTextField(20);

        enterUsernamePanel.add(enterUsernameLabel);
        enterUsernamePanel.add(usernameTextField);
        enterUsernamePanel.add(submitUsername);

        enterUsernamePanel.setVisible(true);
        super.add(enterUsernamePanel);

        submitUsername.addActionListener(e -> {
            this.username = usernameTextField.getText();

            this.kvstore = new KVStore("users/" + this.username, true);

            enterUsernamePanel.setVisible(false);

            this.insertUrl();
        });
    }

    private void insertUrl() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JPanel subPanel1 = new JPanel();
        subPanel1.setLayout(new FlowLayout(FlowLayout.CENTER));

        JLabel label = new JLabel("Enter shopping list URL: ");
        JButton button = new JButton("Submit");
        JButton createShoppingListButton = new JButton("New Shopping List");

        JTextField urlTextField = new JTextField(36);

        List<String> lists = this.kvstore.getLists();

        for (String list : lists) {
            JButton listButton = new JButton(list);

            listButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    Optional<String> retrievedList = kvstore.get(list);

                    if (retrievedList.isPresent()) {
                        try {
                            url = list;
                            showShoppingList(retrievedList.get());
                        } catch (JsonProcessingException ex) {
                            throw new RuntimeException(ex);
                        }

                        dispose();
                    } else {
                        System.err.println("That shopping list does not exist.");
                    }
                }
            });

            subPanel.add(listButton);
        }

        this.itemListPanel = new JPanel();
        this.itemListPanel.setLayout(new BoxLayout(this.itemListPanel, BoxLayout.Y_AXIS));

        button.addActionListener(e -> {
            String inputURL = urlTextField.getText();
            this.url = inputURL;

            // Submit
            this.sl = new ORMap(this.username);

            fetchListFromCloud();

            try {
                this.showShoppingList(this.sl.toJson());
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });

        createShoppingListButton.addActionListener(e -> {
            this.url = UUID.randomUUID().toString();

            ORMap empty = new ORMap(this.username);

            String emptyList = empty.toJson();

            try {
                showShoppingList(emptyList);

                super.dispose();
            } catch (JsonProcessingException ex) {
                System.err.println("Could not display the created shopping list: " + ex);
            }
        });

        subPanel1.add(label);
        subPanel1.add(urlTextField);
        subPanel1.add(button);
        subPanel1.add(createShoppingListButton);

        panel.add(subPanel1);
        panel.add(subPanel);

        super.add(panel);
    }

    private void showShoppingList(String list) throws JsonProcessingException {
        this.sl = ORMap.fromJson(list);

        this.updateItemList();

        JFrame frame = new JFrame(this.username);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(this.width, this.height);
        frame.setLocationRelativeTo(this);

        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout());
        container.add(this.createAddItemPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(this.itemListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        this.addItemPanel.add(new JTextField(this.url));

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
        saveButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_SAVE_PATH)));

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
                fetchListFromCloud();
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

    private void fetchListFromCloud() {
        if (!performReadRequest()) {
            return;
        }

        CompletableFuture<Optional<RequestStatus>> future = CompletableFuture.supplyAsync(() -> performPollRequest());

        Optional<RequestStatus> pollStatus;
        try {
            pollStatus = future.get(4000, TimeUnit.MILLISECONDS);
        } catch ( InterruptedException | java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException ex) {
            future.cancel(true);
            return;
        }

        if (pollStatus.isEmpty() || !pollStatus.get().equals(RequestStatus.DONE)) {
            displayTemporaryMessage(addItemPanel, "Response timeout", 3);

            return;
        }

        Optional<ORMap> fetchedList = performFetchReadDataRequest();

        for (Pair<String, Integer> entry: sl.read()) {
            String k = entry.getFirst();
            int v = entry.getSecond();

        }

        if (fetchedList.isPresent()) {
            for (Pair<String, Integer> entry: fetchedList.get().read()) {
                String k = entry.getFirst();
                int v = entry.getSecond();

            }

            sl.join(fetchedList.get());

            for (Pair<String, Integer> entry: sl.read()) {
                String k = entry.getFirst();
                int v = entry.getSecond();

            }

            updateItemList();
        }
    }
    private void performWriteRequest(String endpoint) {
        for (Pair<String, Integer> entry: this.sl.read()) {
            String k = entry.getFirst();
            int v = entry.getSecond();
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
            JButton deleteButton = this.createButton("X", UIConstants.FONT_MONOSPACED);

            deleteButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_DELETE_PATH)));

            incrementButton.addActionListener(createIncrementActionListener(item));
            decrementButton.addActionListener(createDecrementActionListener(item, quantity));
            deleteButton.addActionListener(createDeleteActionListener(item));

            rightPanel.add(quantityLabel);
            rightPanel.add(incrementButton);
            rightPanel.add(decrementButton);
            rightPanel.add(deleteButton);
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

            this.updateItemList();
        };
    }

    private ActionListener createDecrementActionListener(String itemName, int currentQuantity) {
        return e -> {
            this.sl.put(itemName, currentQuantity - 1);
            this.updateItemList();
        };
    }

    private ActionListener createDeleteActionListener(String itemName) {
        return e -> {
            this.sl.erase(itemName);
            this.updateItemList();
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
