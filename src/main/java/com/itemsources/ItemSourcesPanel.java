package com.itemsources;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;



public class ItemSourcesPanel extends PluginPanel
{
    private final JTextPane contentArea = new JTextPane();
    private final JTextField searchBar = new JTextField();
    private final JLabel headerLabel = new JLabel();

    public ItemSourcesPanel()
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBorder(new EmptyBorder(10, 10, 0, 10));
        northPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel searchLabel = new JLabel("Search");
        searchLabel.setFont(FontManager.getRunescapeFont());
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        searchBar.setForeground(Color.WHITE);
        searchBar.setCaretColor(Color.WHITE);
        searchBar.setPreferredSize(new Dimension(PANEL_WIDTH - 20, 30));

        northPanel.add(searchLabel);
        northPanel.add(searchBar);
        add(northPanel, BorderLayout.NORTH);


        headerLabel.setHorizontalAlignment(JLabel.CENTER);
        headerLabel.setHorizontalTextPosition(JLabel.LEADING);
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setBorder(new EmptyBorder(5, 0, 5, 0));
        add(headerLabel, BorderLayout.CENTER);
        northPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        northPanel.add(headerLabel);

        contentArea.setEditable(false);
        contentArea.setContentType("text/html");
        contentArea.setBackground(ColorScheme.DARK_GRAY_COLOR);

        contentArea.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType()))
            {
                LinkBrowser.browse(e.getURL().toString());
            }
        });

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        // Footer Panel for Buttons
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        southPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        // GitHub Section
        JLabel githubLabel = new JLabel("Have a suggestion? Found a bug?");
        githubLabel.setFont(FontManager.getRunescapeSmallFont());
        githubLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JButton githubButton = new JButton("Item Sources GitHub");
        githubButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        githubButton.addActionListener(e -> LinkBrowser.browse("https://github.com/ByteThis/item-sources-plugin/issues"));

        // Discord Section
        JButton discordButton = new JButton("Discord");
        discordButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        discordButton.addActionListener(e -> LinkBrowser.browse("https://discordapp.com/users/126116650184474625"));

        // Add components with spacing
        southPanel.add(githubLabel);
        southPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        southPanel.add(githubButton);
        southPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        southPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        southPanel.add(discordButton);

        add(southPanel, BorderLayout.SOUTH);
    }

    public void onSearch(ActionListener al) { searchBar.addActionListener(al); }
    public String getSearchText() { return searchBar.getText(); }

    public void updateContent(String title, String htmlContent, java.awt.image.BufferedImage icon)
    {

        headerLabel.setText(title);
        headerLabel.setIcon(icon != null ? new javax.swing.ImageIcon(icon) : null);

        String styledHtml = "<html><body style='font-family:sans-serif; color:white; padding:0 10px 10px 10px;'>" +
                "<hr style='margin: 0 0 10px 0;'>" + // Top line
                htmlContent +
                "</body></html>";

        contentArea.setText(styledHtml);
        contentArea.setCaretPosition(0);
    }
}