package org.C2.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ShoppingListUI {
    private DefaultListModel<String> listModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ShoppingListUI().createAndShowGUI());
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Shopping List Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        listModel = new DefaultListModel<>();

        JList<String> shoppingList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(shoppingList);

        JTextField itemNameTextField = new JTextField(20);
        JButton addButton = new JButton("+");
        JButton removeButton = new JButton("-");

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String itemName = itemNameTextField.getText().trim();

                if (!itemName.isEmpty()) {
                    listModel.addElement(itemName);
                    itemNameTextField.setText("");
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = shoppingList.getSelectedIndex();
                if (selectedIndex == -1) {
                    listModel.remove(selectedIndex);
                }
            }
        });

        frame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel();
        inputPanel.add(new JLabel("Item name: "));
        inputPanel.add(itemNameTextField);
        inputPanel.add(addButton);
        inputPanel.add(removeButton);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(listScrollPane, BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
