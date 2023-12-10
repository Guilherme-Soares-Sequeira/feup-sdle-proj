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
    private JPanel itemListPanel;

    public MockUI() {
        this.kvstore = new KVStore("users", true);

        this.loadUI();

        this.width = 800;
        this.height = 500;

        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setTitle("Shopping List");
        super.setSize(500, 500);
        super.setLocationRelativeTo(null);
        super.setVisible(true);
    }

    private void loadUI() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter shopping list URL: ");
        JButton button = new JButton("Submit");
        JTextField urlTextField = new JTextField(20);

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

        panel.add(label);
        panel.add(urlTextField);
        panel.add(button);

        super.add(panel);
    }

    private void showShoppingList(String list) throws JsonProcessingException {
        this.sl = ORMap.fromJson(list);

        this.updateItemList();

        JFrame frame = new JFrame("Shopping List Content");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(this.width, this.height);
        frame.setLocationRelativeTo(this);

        Container container = frame.getContentPane();
        container.setLayout(new BorderLayout());
        container.add(this.createAddItemPanel(), BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(this.itemListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        container.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JPanel createAddItemPanel() {
        JPanel addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addItemPanel.setBackground(Color.LIGHT_GRAY);

        JTextField itemNameField = new JTextField(20);
        itemNameField.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel itemNameLabel = new JLabel("Item name: ");
        itemNameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton addItemButton = this.createButton("ADD", Font.MONOSPACED);
        JButton pullButton = this.createButton("PULL", Font.MONOSPACED);
        JButton pushButton = this.createButton("PUSH", Font.MONOSPACED);

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
                if (!performReadRequest()) {
                    return;
                }

                CompletableFuture<Optional<RequestStatus>> future = CompletableFuture.supplyAsync(() -> performPollRequest());

                Optional<RequestStatus> pollStatus;
                try {
                    pollStatus = future.get(4000, TimeUnit.MILLISECONDS);
                } catch (Exception ex) {
                    // request timed out or was interrupted

                    // TODO: add a message in the UI
                    return;
                }

                if (pollStatus.isEmpty() || !pollStatus.get().equals(RequestStatus.DONE)) {
                    // response wasn't ready in time

                    // TODO: add a message in the UI

                    return;
                }

                Optional<ORMap> fetchedList = performFetchReadDataRequest();

                if (fetchedList.isPresent()) {
                    System.out.println("I got a list from the read request");

                    for (Pair<String, Integer> entry: sl.read()) {
                        String k = entry.getFirst();
                        String v = entry.getFirst();

                        System.out.println("k: " + k);
                        System.out.println("v: " + v);
                    }

                    sl.join(fetchedList.get());

                    updateItemList();
                }

            }
        });

        addItemPanel.add(itemNameLabel);
        addItemPanel.add(itemNameField);
        addItemPanel.add(addItemButton);
        addItemPanel.add(pullButton);
        addItemPanel.add(pushButton);

        return addItemPanel;
    }

    private void performWriteRequest(String endpoint) {
        HttpResult<String> result = ServerRequests.requestWrite(LoadBalancer.lbinfo, url, this.sl);

        if (!result.isOk()) {
            System.err.println(result.errorMessage());
        } else {
            this.forID = result.get();
        }
    }

    private boolean performReadRequest() {
        HttpResult<String> result = ServerRequests.requestRead(LoadBalancer.lbinfo, this.url);

        this.forID = forID;

        return result.isOk();
    }

    private Optional<RequestStatus> performPollRequest() {
        HttpResult<RequestStatus> result = ServerRequests.pollRequest(LoadBalancer.lbinfo, this.forID);

        if (!result.isOk()) {
            System.err.println(result.errorMessage());
            return Optional.empty();
        }

        return Optional.of(result.get());
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
            this.sl.put(name, 0);
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


    private URL loadIcon(String path) {
        return Objects.requireNonNull(getClass().getResource(path));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MockUI::new);
    }
}
