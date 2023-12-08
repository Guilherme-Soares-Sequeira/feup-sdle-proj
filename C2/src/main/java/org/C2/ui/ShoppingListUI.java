package org.C2.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        this.addButton = new JButton("ADD");
        this.itemListPanel = new JPanel();
    }

    public ShoppingListUI() {
        this.init();

        this.loadAddButton();

        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        JLabel itemNameLabel = new JLabel("Item name: ");

        itemNameLabel.setFont(new Font("Times New Roman", Font.BOLD, 14));
        panel.add(itemNameLabel);
        panel.setBackground(Color.LIGHT_GRAY);
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

    private void loadAddButton() {
        addButton.setBackground(new Color(35, 112, 199));
        addButton.setForeground(Color.WHITE);
        addButton.setBorderPainted(false);
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
