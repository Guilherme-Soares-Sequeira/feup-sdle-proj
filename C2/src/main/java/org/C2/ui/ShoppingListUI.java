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
    private JPanel itemListPanel;

    private void init() {
        this.shoppingList = new HashMap<>();
        this.itemNameField = new JTextField(20);
        this.itemListPanel = new JPanel();
        this.addButton = new JButton("ADD");
    }

    public ShoppingListUI() {
        this.init();

        this.loadAddButton();

        this.itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        JLabel itemNameLabel = new JLabel("Item name: ");

        itemNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(itemNameLabel);
        panel.setBackground(Color.LIGHT_GRAY);
        panel.add(this.itemNameField);
        panel.add(this.addButton);

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(panel, BorderLayout.NORTH);
        container.add(new JScrollPane(this.itemListPanel), BorderLayout.CENTER);

        setTitle("Shopping List Application");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadAddButton() {
        addButton.setBackground(Color.WHITE);
        addButton.setForeground(Color.BLACK);
        addButton.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        addButton.setFont(new Font("Arial", Font.BOLD, 14));
        addButton.setFocusPainted(false);
        addButton.addActionListener(e -> addItem());
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
            JPanel itemContainer = new JPanel(new BorderLayout()); // Use BorderLayout for alignment

            String itemName = entry.getKey();
            int quantity = entry.getValue();

            // Left side: item name
            JLabel nameLabel = new JLabel(itemName);
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leftPanel.add(nameLabel);
            itemContainer.add(leftPanel, BorderLayout.WEST);

            // Right side: quantity and buttons
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JLabel quantityLabel = new JLabel("Quantity: " + quantity);
            JButton incrementButton = new JButton("+");
            JButton decrementButton = new JButton("-");

            incrementButton.addActionListener(createIncrementActionListener(itemName));
            decrementButton.addActionListener(createDecrementActionListener(itemName, quantity));

            rightPanel.add(quantityLabel);
            rightPanel.add(incrementButton);
            rightPanel.add(decrementButton);
            itemContainer.add(rightPanel, BorderLayout.EAST);

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
