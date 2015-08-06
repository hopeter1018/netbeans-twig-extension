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
import org.netbeans.modules.php.twigplugin.Utils.CommonConstants;
import org.netbeans.modules.php.twigplugin.Utils.ProjectUtils;
import org.openide.filesystems.FileObject;

/**
 *
 * @author peter.ho
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG, service = CompletionProvider.class),
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG_BLOCK, service = CompletionProvider.class)
})
public class IncludeEmbedExtendsCompleter implements CompletionProvider {

    final static Pattern commentPattern = Pattern.compile("\\{\\#(.*)\\#\\}");

    public static Map<String, CodeCompleterUtils.OptionsItem> getAllTwigSimpleFilter(Project editingProject) {
        List<FileObject> twigFiles = ProjectUtils.findByMimeType(editingProject, CommonConstants.NB_MIME_TWIG);
        List<FileObject> editingDirs = ProjectUtils.getEditingPaths(editingProject);

        Map<String, CodeCompleterUtils.OptionsItem> result = new HashMap<String, CodeCompleterUtils.OptionsItem>();

        for (FileObject twigFile : twigFiles) {
            String text;
            try {
                text = twigFile.asText();
                Matcher matcher = commentPattern.matcher(text);
                String name = twigFile.getPath().replace(".twig", "");

                for (FileObject editingDir : editingDirs) {
                    if (editingDir != null) {
                        name = name.replace(editingDir.getPath(), "");
                    }
                }
                String comments = "-- No comments found in the " + name + ".twig --";
                if (matcher.find()) {
                    comments = matcher.group(1);
                }
                result.put(name, new CodeCompleterUtils.OptionsItem(name, comments + CodeCompleterUtils.getFileObjectInfo(twigFile)));
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
                String WRAPPER = "{% ";
                String PREFIX_IMPORT = "i";
                String PREFIX_USE = "u";
                String PREFIX_EMBED = "e";
                String PREFIX_EXTEND = "e";

                Map<String, CodeCompleterUtils.OptionsItem> options = getAllTwigSimpleFilter(CodeCompleterUtils.getEditingProject());

                int startOffset = caretOffset - 1;
                String docText = "";
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    docText = bDoc.getText(0, bDoc.getLength());
                    final int lineStartOffset = CodeCompleterUtils.getLastIndexOfDoc(bDoc, caretOffset, WRAPPER);
                    if (caretOffset - lineStartOffset - WRAPPER.length() >= 0) {
                        final char[] line = bDoc.getText(lineStartOffset + WRAPPER.length(), caretOffset - lineStartOffset - WRAPPER.length()).toCharArray();
                        filter = new String(line);
                    }
                    startOffset = lineStartOffset;
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }

                if (filter == null || !(filter.startsWith(PREFIX_IMPORT) || filter.startsWith(PREFIX_USE) || filter.startsWith(PREFIX_EMBED) || filter.startsWith(PREFIX_EXTEND))) {
                    completionResultSet.finish();
                    return;
                }

                for (Map.Entry<String, CodeCompleterUtils.OptionsItem> option : options.entrySet()) {
                    if ((PREFIX_IMPORT + "mport " + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(
                                (docText.contains(PREFIX_IMPORT + "mport " + option.getKey())),
                                "Twig import",
                                PREFIX_IMPORT + "mport " + option.getKey(),
                                WRAPPER + PREFIX_IMPORT + "mport " + "\"" + option.getKey() + "\" as " + option.getKey() + " %}\r\n",
                                option.getValue(),
                                startOffset,
                                caretOffset
                        ));
                    } else if ((PREFIX_USE + "se " + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(
                                (docText.contains(PREFIX_USE + "se " + option.getKey())),
                                "Twig use",
                                PREFIX_USE + "se " + option.getKey(),
                                WRAPPER + PREFIX_USE + "se " + "\"" + option.getKey() + "\" %}\r\n",
                                option.getValue(),
                                startOffset,
                                caretOffset
                        ));
                    } else if ((PREFIX_EMBED + "mbed " + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(
                                (docText.contains(PREFIX_EMBED + "mbed " + option.getKey())),
                                "Twig embed",
                                PREFIX_EMBED + "mbed " + option.getKey(),
                                WRAPPER + PREFIX_EMBED + "mbed " + "\"" + option.getKey() + "\" %}\r\n{% endembed %}",
                                option.getValue(),
                                startOffset,
                                caretOffset
                        ));
                    } else if ((PREFIX_EXTEND + "xtends " + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(
                                (docText.contains(PREFIX_EXTEND + "xtends " + option.getKey())),
                                "Twig extends",
                                PREFIX_EXTEND + "xtends " + option.getKey() + ".twig",
                                WRAPPER + PREFIX_EXTEND + "xtends " + "\"" + option.getKey() + ".twig\" %}\r\n",
                                option.getValue(),
                                startOffset,
                                caretOffset));
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
}
