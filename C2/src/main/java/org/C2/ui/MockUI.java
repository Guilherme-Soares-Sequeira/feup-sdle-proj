package org.C2.ui;

import org.C2.cloud.database.KVStore;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class MockUI extends JFrame {
    private final KVStore kvstore;
    private final int width;
    private final int height;
    private String url;
    private JSONObject sl;
    private JPanel itemListPanel;

    public MockUI() {
        this.kvstore = new KVStore("kvstore", true);

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
                this.showShoppingList(list.get());

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

    private void showShoppingList(String list) {
        this.sl = new JSONObject(new JSONTokener(list));

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
                System.out.println("I got called and I am pushing content");
                kvstore.put(url, sl.toString());
            }
        });

        addItemPanel.add(itemNameLabel);
        addItemPanel.add(itemNameField);
        addItemPanel.add(addItemButton);
        addItemPanel.add(pullButton);
        addItemPanel.add(pushButton);

        return addItemPanel;
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

        for (String item : this.sl.keySet()) {
            JPanel itemContainer = new JPanel(new BorderLayout());

            int quantity = (int) this.sl.get(item);

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
            int currentQuantity = (int) this.sl.get(itemName);
            this.sl.put(itemName, currentQuantity + 1);
            updateItemList();
        };
    }

    private ActionListener createDecrementActionListener(String itemName, int currentQuantity) {
        return e -> {
            if (currentQuantity > 0) {
                this.sl.put(itemName, currentQuantity - 1);
                updateItemList();
            }
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
