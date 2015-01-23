/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin.Utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JToolTip;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author peter.ho
 */
public class GeneralCompletionItem implements CompletionItem {

    private boolean exists = false;
    private final String text;
    private final String insertText;
    private final String groupName;
    private final CodeCompleterUtils.OptionsItem optionsItem;
    private static final ImageIcon fieldIcon
            = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/php/twigplugin/Utils/icon.png", false));
    private static final Color fieldColor = Color.decode("0x000000");
    private final int caretOffset;
    private final int dotOffset;

    public GeneralCompletionItem(boolean exists, String groupName, String text, CodeCompleterUtils.OptionsItem phpClass, int dotOffset, int caretOffset) {
        this.exists = exists;
        this.groupName = "ZMS5 " + groupName;
        this.insertText = text;
        this.text = text;
        this.optionsItem = phpClass;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
    }

    public GeneralCompletionItem(boolean exists, String groupName, String text, String insertText, CodeCompleterUtils.OptionsItem phpClass, int dotOffset, int caretOffset) {
        this.exists = exists;
        this.groupName = "ZMS5 " + groupName;
        this.text = text;
        this.insertText = insertText;
        this.optionsItem = phpClass;
        this.dotOffset = dotOffset;
        this.caretOffset = caretOffset;
    }

    @Override
    public void defaultAction(JTextComponent jTextComponent) {
        try {
            StyledDocument doc = (StyledDocument) jTextComponent.getDocument();
            boolean toInsert = true;
            if (this.exists && JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null, insertText + "\r\nAlready exists in source.\r\nAre you sure to insert?")) {
                toInsert = false;
            }
            if (toInsert) {
                doc.remove(dotOffset, caretOffset - dotOffset);
                doc.insertString(dotOffset, insertText, null);
            }

            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void processKeyEvent(KeyEvent arg0) {
    }

    @Override
    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(text, groupName, graphics, font);
    }

    @Override
    public void render(Graphics g, Font defaultFont, Color defaultColor,
            Color backgroundColor, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, text, groupName, g, defaultFont,
                (selected ? Color.white : fieldColor), width, height, selected);
    }

    @Override
    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                completionResultSet.setDocumentation(new GerenalCompletionDocumentation(GeneralCompletionItem.this));
                completionResultSet.finish();
            }
        });
    }

    @Override
    public CompletionTask createToolTipTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                JToolTip toolTip = new JToolTip();
                toolTip.setTipText("Press Enter to insert \"" + text + "\"");
                completionResultSet.setToolTip(toolTip);
                completionResultSet.finish();
            }
        });
    }

    @Override
    public boolean instantSubstitution(JTextComponent arg0) {
        return false;
    }

    @Override
    public int getSortPriority() {
        return 0;
    }

    @Override
    public CharSequence getSortText() {
        return text.substring(1);
    }

    public CodeCompleterUtils.OptionsItem getOptionsItem() {
        return optionsItem;
    }

    public String getText() {
        return text;
    }

    @Override
    public CharSequence getInsertPrefix() {
        return text;
    }
}
