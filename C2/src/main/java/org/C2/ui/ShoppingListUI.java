package org.C2.ui;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class ShoppingListUI extends JFrame {
    private final Map<String, Integer> shoppingList;
    private final JTextField itemNameField;
    private final JButton addButton;

    private final JButton pullButton;
    private final JButton pushButton;

    private final JPanel addItemPanel;
    private final JPanel itemListPanel;

    public ShoppingListUI() {
        this.shoppingList = new HashMap<>();

        this.addItemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        this.itemNameField = new JTextField(UIConstants.ITEM_NAME_TEXT_FIELD_COLS);

        this.itemListPanel = new JPanel();

        this.itemListPanel.setLayout(new BoxLayout(this.itemListPanel, BoxLayout.Y_AXIS));

        this.addButton = this.createButton(UIConstants.ADD_ITEM_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);
        this.addButton.addActionListener(e -> this.addItem());
        this.pullButton = this.createButton(UIConstants.PULL_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);
        this.pushButton = this.createButton(UIConstants.PUSH_BUTTON_TEXT, UIConstants.FONT_MONOSPACED);

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

        Icon pullIcon = new ImageIcon("/home/pedro-ramalho/uni/feup-sdle-proj/C2/assets/download24.png");
        Icon pushIcon = new ImageIcon("/home/pedro-ramalho/uni/feup-sdle-proj/C2/assets/upload24.png");

        this.pullButton.setIcon(pullIcon);
        this.pushButton.setIcon(pushIcon);

        this.addItemPanel.add(this.pullButton);
        this.addItemPanel.add(this.pushButton);
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
