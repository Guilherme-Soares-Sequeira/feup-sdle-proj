package org.C2.ui;

import javax.swing.*;
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

    public ShoppingListUI() {
        shoppingList = new HashMap<>();

        itemNameField = new JTextField(20);
        addButton = new JButton("Add");
        itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addItem();
            }
        });

        JPanel panel = new JPanel();
        panel.add(new JLabel("Item name:"));
        panel.add(itemNameField);
        panel.add(addButton);

        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(panel, BorderLayout.NORTH);
        container.add(new JScrollPane(itemListPanel), BorderLayout.CENTER);

        setTitle("Shopping List Application");
        setSize(1280, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);
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
            JPanel itemPanel = new JPanel();
            itemPanel.setLayout(new FlowLayout());

            String itemName = entry.getKey();
            int quantity = entry.getValue();

            JLabel nameLabel = new JLabel(itemName);
            JLabel quantityLabel = new JLabel("Quantity: " + quantity); // Display quantity

            itemPanel.add(nameLabel);
            itemPanel.add(quantityLabel);

            JButton incrementButton = new JButton("+");
            incrementButton.addActionListener(createIncrementActionListener(itemName));

            JButton decrementButton = new JButton("-");
            decrementButton.addActionListener(createDecrementActionListener(itemName, quantity));

            itemPanel.add(incrementButton);
            itemPanel.add(decrementButton);

            itemListPanel.add(itemPanel);
        }

        itemListPanel.revalidate();
        itemListPanel.repaint();
    }

    private ActionListener createIncrementActionListener(String itemName) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentQuantity = shoppingList.get(itemName);
                shoppingList.put(itemName, currentQuantity + 1);
                updateItemList();
            }
        };
    }

    private ActionListener createDecrementActionListener(String itemName, int currentQuantity) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentQuantity > 0) {
                    shoppingList.put(itemName, currentQuantity - 1);
                    updateItemList();
                }
            }
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ShoppingListUI();
            }
        });
    }
}
