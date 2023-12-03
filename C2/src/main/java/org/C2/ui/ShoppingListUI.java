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
    private JTextArea itemListArea;

    public ShoppingListUI() {
        shoppingList = new HashMap<>();

        itemNameField = new JTextField(20);
        addButton = new JButton("Add");
        itemListArea = new JTextArea(10, 30);

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
        container.add(new JScrollPane(itemListArea), BorderLayout.CENTER);

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
        itemListArea.removeAll();
        itemListArea.setText("");

        for (Map.Entry<String, Integer> entry : shoppingList.entrySet()) {
            JPanel itemPanel = new JPanel();

            String itemName = entry.getKey();
            int quantity = entry.getValue();

            itemPanel.add(new JLabel(itemName));

            itemListArea.append(entry.getKey() + " - Quantity: " + entry.getValue() + "\n");
        }
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
