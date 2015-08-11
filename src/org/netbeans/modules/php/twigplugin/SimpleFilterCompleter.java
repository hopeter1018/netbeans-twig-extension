/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin;

import org.netbeans.modules.php.twigplugin.Utils.GeneralCompletionItem;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.util.Exceptions;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.mimelookup.MimeRegistrations;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.twigplugin.Utils.CodeCompleterUtils;
import org.openide.filesystems.FileObject;
import org.netbeans.modules.php.twigplugin.Utils.CommonConstants;

/**
 *
 * @author peter.ho
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG, service = CompletionProvider.class),
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG_BLOCK, service = CompletionProvider.class)
})
public class SimpleFilterCompleter implements CompletionProvider {

    final static Pattern twigSimpleFilterPattern = Pattern.compile("new Twig_SimpleFilter\\("
                        + "[\'\"](?<name>[^\'\"]+)[\'\"]"
                        + "[^\'\"]*"
                        + "[\'\"](?<string1>[^\'\"]+)");

    public static Map<String, CodeCompleterUtils.OptionsItem> getAllTwigSimpleFilter(Project editingProject) {
        List<FileObject> phpFiles = TwigCache.getPhp(
            "twigfilter",
            new TwigCacheAttributeChecker(){
                @Override
                public boolean isMatched(FileObject file) {
                    boolean matched = false;
                    try {
                        matched = twigSimpleFilterPattern.matcher(file.asText()).find();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    return matched;
                }
            });
//        List<FileObject> phpFiles = TwigCache.getPhp();
        Map<String, CodeCompleterUtils.OptionsItem> result = new HashMap<String, CodeCompleterUtils.OptionsItem>();

        for (FileObject phpFile : phpFiles) {
            String text;
            try {
                text = phpFile.asText();
                Matcher matcher = twigSimpleFilterPattern.matcher(text);
                while (matcher.find())
                {
                    String name = matcher.group("name");
                    String method = matcher.group("string1");

                    int functionStart = text.indexOf("function " + method);
                    int docEnd = text.lastIndexOf("*/", functionStart);
                    int docStart = text.lastIndexOf("/**", functionStart) + 4;
                    String content = text.substring(docStart, docEnd);

                    result.put(name, new CodeCompleterUtils.OptionsItem(name, "<pre>" + content.replaceAll("(?s)[ ]+[*][ ]+", "") + "</pre>"));
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return result;
    }

    @Override
    public CompletionTask createTask(int i, JTextComponent jTextComponent) {
        if (i != CompletionProvider.COMPLETION_QUERY_TYPE) {
            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
                String filter = null;
                String PREFIX = "";

                Map<String, CodeCompleterUtils.OptionsItem> options = getAllTwigSimpleFilter(CodeCompleterUtils.getEditingProject());

                int startOffset = caretOffset - 1;
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    final int lineStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
                    final char[] line = bDoc.getText(lineStartOffset, caretOffset - lineStartOffset).toCharArray();
                    final int whiteOffset = indexOfWhite(line);
                    filter = new String(line, whiteOffset + 1, line.length - whiteOffset - 1);
                    if (whiteOffset > 0) {
                        startOffset = lineStartOffset + whiteOffset + 1;
                    } else {
                        startOffset = lineStartOffset;
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }

                if (filter == null || !filter.startsWith(PREFIX)) {
                    completionResultSet.finish();
                    return;
                }

                for (Map.Entry<String, CodeCompleterUtils.OptionsItem> option : options.entrySet()) {
                    if ((PREFIX + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(false, "Twig Filter", PREFIX + option.getKey(), option.getValue(), startOffset, caretOffset));
                    }
                }
                completionResultSet.finish();
            }
        }, jTextComponent);

    }

    @Override
    public int getAutoQueryTypes(JTextComponent arg0, String arg1) {
        return 0;
    }

    static int getRowFirstNonWhite(StyledDocument doc, int offset)
            throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1)
                        + ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    static int indexOfWhite(char[] line) {
        int i = line.length;
        while (--i > -1) {
            final char c = line[i];
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }
}
