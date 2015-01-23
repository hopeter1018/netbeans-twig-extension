/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.openide.awt.StatusDisplayer;
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
public class IncludeEmbedHyperlinkProvider implements HyperlinkProviderExt {

    private int startOffset, endOffset;
    final Pattern pattern = Pattern.compile("(?<type>embed|include|extends|import|use)(?<spaces> +)(\\\"|\\')(?<filename>[^\\\"|\\']+)(\\\"|\\')");

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
            String text = doc.getText(startOffset, endOffset - startOffset);
            FileObject fo = getFileObject(doc);
            String pathToFileToOpen = HyperlinkProviderUtils.getTwigCommonFile(text);
            if (pathToFileToOpen != null) {
                File fileToOpen = FileUtil.normalizeFile(new File(pathToFileToOpen));
                if (fileToOpen.exists()) {
                    try {
                        FileObject foToOpen = FileUtil.toFileObject(fileToOpen);
                        DataObject dObj = DataObject.find(foToOpen);
                        dObj.getLookup().lookup(OpenCookie.class).open();
                    } catch (DataObjectNotFoundException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else {
                    StatusDisplayer.getDefault().setStatusText(fileToOpen.getName() + " (" + pathToFileToOpen + ") doesn't exist!");
                }
            } else {
                StatusDisplayer.getDefault().setStatusText(text + " doesn't exist!");
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static FileObject getFileObject(Document doc) {
        DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
        return od != null ? od.getPrimaryFile() : null;
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

                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        int offsetPrefix = matcher.group("type").length() + matcher.group("spaces").length() + 1;
                        startOffset = startOffset + offsetPrefix;
                        endOffset = startOffset + matcher.group("filename").length();
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
