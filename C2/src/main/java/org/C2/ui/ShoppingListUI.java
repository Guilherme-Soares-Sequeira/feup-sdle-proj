package org.C2.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ShoppingListUI extends JFrame {
    private Map<String, Integer> shoppingList;
    private JTextField itemNameField;
    private JButton addButton;

    private JPanel addItemPanel;
    private JPanel itemListPanel;

    public ShoppingListUI() {
        this.shoppingList = new HashMap<>();

        this.addItemPanel = new JPanel();
        this.itemNameField = new JTextField(UIConstants.ITEM_NAME_TEXT_FIELD_COLS);

        this.itemListPanel = new JPanel();

        GridLayout layout = new GridLayout(10, 1, 2, 2);

        this.itemListPanel.setLayout(new BoxLayout(this.itemListPanel, BoxLayout.Y_AXIS));

        this.addButton = this.createButton(UIConstants.ADD_ITEM_BUTTON_TEXT, UIConstants.FONT_ARIAL);
        this.addButton.addActionListener(e -> this.addItem());

        this.loadAddItemComponent();

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(this.addItemPanel, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(this.itemListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        container.add(scrollPane, BorderLayout.CENTER);

        setTitle("Shopping List Application");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private void loadAddItemComponent() {
        JLabel itemNameLabel = new JLabel(UIConstants.ITEM_NAME_TEXT_FIELD_TEXT);
        itemNameLabel.setFont(new Font(UIConstants.FONT_ARIAL, Font.BOLD, 14));

        this.addItemPanel.add(itemNameLabel);
        this.addItemPanel.setBackground(Color.LIGHT_GRAY);
        this.addItemPanel.add(this.itemNameField);
        this.addItemPanel.add(this.addButton);
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
            updateItemList();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ShoppingListUI::new);
    }
}
