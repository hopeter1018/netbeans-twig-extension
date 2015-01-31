/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProviderExt;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkType;
import org.netbeans.modules.php.twigplugin.Utils.HyperlinkProviderUtils;
import org.netbeans.modules.php.twigplugin.Utils.CommonConstants;
import org.netbeans.modules.php.twigplugin.Utils.ProjectUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 *
 * @author peter.ho
 */
@MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG, service = HyperlinkProviderExt.class)
public class ExtendsBlockHyperlinkProvider implements HyperlinkProviderExt {

    private int startOffset, endOffset;
    private String blockName;
    final static Pattern blockPattern = Pattern.compile("(?<type>block)(?<spaces> +)(?<blockname>.*)");
    final static Pattern extendsNamePattern = Pattern.compile("\\{%( +)extends( +)\\\"(?<extendsname>[^\\\"]+)\\\"( +)%\\}");

    @Override
    public Set<HyperlinkType> getSupportedHyperlinkTypes() {
        return EnumSet.of(HyperlinkType.GO_TO_DECLARATION);
    }

    @Override
    public boolean isHyperlinkPoint(Document doc, int offset, HyperlinkType type) {
        return getHyperlinkSpan(doc, offset, type) != null;
    }

    @Override
    public int[] getHyperlinkSpan(Document doc, int offset, HyperlinkType type) {
        return getIdentifierSpan(doc, offset);
    }

    @Override
    public String getTooltipText(Document doc, int offset, HyperlinkType type) {
        String text = null;
        try {
            text = doc.getText(startOffset, endOffset - startOffset);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return "Click to open " + text;
    }

    @Override
    public void performClickAction(Document doc, int offset, HyperlinkType ht) {
        try {
            String text = getExtendsName(doc);
            String pathToFileToOpen = HyperlinkProviderUtils.getTwigCommonFile(text);

            if (pathToFileToOpen != null) {
                File fileToOpen = FileUtil.normalizeFile(new File(pathToFileToOpen));
                if (fileToOpen.exists()) {
                    try {
                        FileObject foToOpen = FileUtil.toFileObject(fileToOpen);
                        DataObject dObj = DataObject.find(foToOpen);
                        dObj.getLookup().lookup(OpenCookie.class).open();
                        final EditorCookie.Observable ec = (EditorCookie.Observable) dObj.getLookup().lookup(EditorCookie.Observable.class);
                        final JEditorPane[] panes = ec.getOpenedPanes();
                        if ((panes != null) && (panes.length > 0)) {
                            panes[0].setCaretPosition(getBlockOffset(foToOpen, blockName));
                        }
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else {
                    StatusDisplayer.getDefault().setStatusText(fileToOpen.getName() + " (" + pathToFileToOpen + ") doesn't exist!");
                }
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        return od != null ? od.getPrimaryFile() : null;
    }

    private String getExtendsName(Document doc) throws BadLocationException {
        String text = doc.getText(0, doc.getLength());
        Matcher matcher = extendsNamePattern.matcher(text);
        while(matcher.find()) {
            System.out.println("matcher.find !");
            return matcher.group("extendsname");
        }
        return null;
    }

    private int getBlockOffset(FileObject foToOpen, String blockName) throws BadLocationException, IOException {
        String text = foToOpen.asText();
        TokenHierarchy<?> th = TokenHierarchy.create(text, Language.find(CommonConstants.NB_MIME_TWIG));
        TokenSequence ts = th.tokenSequence();
        if (ts == null) {
            return 0;
        }

        Pattern pattern = Pattern.compile("\\{%( +)block( +)" + blockName + "( +)%\\}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int offset = matcher.start();
            ts.move(offset);
            if (!ts.moveNext()) {
                return 0;
            }
            Token t = ts.token();
            if (t.id().name().equals("T_TWIG_BLOCK_START")) {
                return offset;
            }
        }
        return 0;
    }

    private int[] getIdentifierSpan(Document doc, int offset) {
        TokenHierarchy<?> th = TokenHierarchy.get(doc);
        TokenSequence ts = th.tokenSequence(Language.find(CommonConstants.NB_MIME_TWIG));
        if (ts == null) {
            return null;
        }
        ts.move(offset);
        if (!ts.moveNext()) {
            return null;
        }
        Token t = ts.token();
        if (t.id().name().equals("T_TWIG_BLOCK")) {
            //Correction for quotation marks around the token:
            startOffset = ts.offset() + 1;
            endOffset = ts.offset() + t.length() - 1;
            //Check that the previous token was an import statement,
            //otherwise we don't want our string literal hyperlinked:
            ts.movePrevious();
            Token prevToken = ts.token();
            if (prevToken.id().name().equals("T_TWIG_BLOCK_START")) {
                try {
                    String text = doc.getText(startOffset, endOffset - startOffset);

                    Matcher matcher = blockPattern.matcher(text);
                    if (matcher.find()) {
                        int offsetPrefix = matcher.group("type").length() + matcher.group("spaces").length();
                        startOffset = startOffset + offsetPrefix;
                        blockName = matcher.group("blockname");
                        endOffset = startOffset + blockName.length();
                        return new int[]{startOffset, endOffset};
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return null;
            } else {
                return null;
            }
        }
        return null;
    }

}
