package org.C2.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ShoppingListUI extends JFrame {
    private final Map<String, Integer> shoppingList;
    private JTextField itemNameField;

    private JButton addButton;
    private JButton pullButton;
    private JButton pushButton;

    private JPanel addItemPanel;
    private JPanel itemListPanel;

    public ShoppingListUI() {
        this.shoppingList = new HashMap<>();

        this.loadButtons();
        this.loadIcons();
        this.loadActionListeners();

        this.loadComponents();

        this.loadFrameProperties();
    }

    private void loadComponents() {
        this.loadAddItemComponent();
        this.loadItemListComponent();
    }

    private void loadFrameProperties() {
        super.setTitle("Shopping List Application");
        super.setSize(1280, 720);
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setLocationRelativeTo(null);
        super.setResizable(false);
        super.setVisible(true);
    }

    private void loadButtons() {
        this.addButton = this.createButton(UIConstants.ADD_ITEM_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);
        this.pullButton = this.createButton(UIConstants.PULL_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);
        this.pushButton = this.createButton(UIConstants.PUSH_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);
    }

    private void loadAddItemComponent() {
        this.addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.addItemPanel.setBackground(Color.LIGHT_GRAY);

        this.itemNameField = new JTextField(UIConstants.ITEM_NAME_TEXT_FIELD_COLS);

        JLabel itemNameLabel = new JLabel(UIConstants.ITEM_NAME_TEXT_FIELD_TEXT);
        itemNameLabel.setFont(new Font(UIConstants.FONT_ARIAL, Font.BOLD, 14));

        this.addItemPanel.add(itemNameLabel);
        this.addItemPanel.add(this.itemNameField);
        this.addItemPanel.add(this.addButton);
        this.addItemPanel.add(this.pullButton);
        this.addItemPanel.add(this.pushButton);
    }

    private void loadItemListComponent() {
        this.itemListPanel = new JPanel();
        this.itemListPanel.setLayout(new BoxLayout(this.itemListPanel, BoxLayout.Y_AXIS));

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(this.addItemPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(this.itemListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        container.add(scrollPane, BorderLayout.CENTER);
    }

    private void loadIcons() {
        this.addButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_ADD_PATH)));
        this.pullButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_PULL_PATH)));
        this.pushButton.setIcon(new ImageIcon(this.loadIcon(UIConstants.ICON_PUSH_PATH)));
    }

    private void loadActionListeners() {
        this.addButton.addActionListener(e -> this.addItem());
        this.pullButton.addActionListener(e -> this.mockCloudButtonActionListener());
        this.pushButton.addActionListener(e -> this.mockCloudButtonActionListener());
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

    private void addItem() {
        String itemName = itemNameField.getText().trim();

        if (!itemName.isEmpty()) {
            shoppingList.put(itemName, 0);
            this.updateItemList();
            itemNameField.setText("");
        }
    }

    private void updateItemList() {
        itemListPanel.removeAll();

        for (Map.Entry<String, Integer> entry : shoppingList.entrySet()) {
            JPanel itemContainer = new JPanel(new BorderLayout());

            String itemName = entry.getKey();
            int quantity = entry.getValue();

            JLabel nameLabel = new JLabel(itemName);
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leftPanel.add(nameLabel);
            itemContainer.add(leftPanel, BorderLayout.WEST);

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JLabel quantityLabel = new JLabel("Quantity: " + quantity);
            JButton incrementButton = this.createButton("+", UIConstants.FONT_MONOSPACED);
            JButton decrementButton = this.createButton("-", UIConstants.FONT_MONOSPACED);

            incrementButton.addActionListener(createIncrementActionListener(itemName));
            decrementButton.addActionListener(createDecrementActionListener(itemName, quantity));

            rightPanel.add(quantityLabel);
            rightPanel.add(incrementButton);
            rightPanel.add(decrementButton);
            itemContainer.add(rightPanel, BorderLayout.EAST);

            itemContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            itemListPanel.add(itemContainer);
        }

        itemListPanel.revalidate();
        itemListPanel.repaint();
    }

    private ActionListener createIncrementActionListener(String itemName) {
        return e -> {
            int currentQuantity = shoppingList.get(itemName);
            shoppingList.put(itemName, currentQuantity + 1);
            updateItemList();
        };
    }

    private ActionListener createDecrementActionListener(String itemName, int currentQuantity) {
        return e -> {
            if (currentQuantity > 0) {
                shoppingList.put(itemName, currentQuantity - 1);
                updateItemList();
            }
        };
    }

    private ActionListener mockCloudButtonActionListener() {
        /*
            TODO: The action listeners for both PULL and PUSH need to be implemented, with the help of the load balancer.
        */
        return e -> System.out.println("Warning: my appropriate ActionListener is missing!");
    }

    private URL loadIcon(String path) {
        return Objects.requireNonNull(getClass().getResource(path));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ShoppingListUI::new);
    }
}
