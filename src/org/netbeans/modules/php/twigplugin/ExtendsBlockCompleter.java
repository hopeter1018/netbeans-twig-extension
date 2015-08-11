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
import org.netbeans.modules.php.twigplugin.Utils.FileSystemUtils;
import org.netbeans.modules.php.twigplugin.Utils.CommonConstants;
import org.netbeans.modules.php.twigplugin.Utils.MyProjectUtils;
import org.openide.filesystems.FileObject;

/**
 *
 * @author peter.ho
 */
@MimeRegistrations({
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG, service = CompletionProvider.class),
    @MimeRegistration(mimeType = CommonConstants.NB_MIME_TWIG_BLOCK, service = CompletionProvider.class)
})
public class ExtendsBlockCompleter implements CompletionProvider {

    final static Pattern haveExtendsPattern = Pattern.compile("\\{\\%[ ]+extends[ ]+\"(?<fileName>[^\"]+)\"[ ]+\\%\\}");
    final static Pattern blockCommentPattern = Pattern.compile("(?<comments>\\{\\#(.*)\\#\\})*(\\s*)\\{\\%[ ]+block[ ]+(?<name>[a-zA-Z0-9\\.]+)[ ]+\\%\\}");

    public static Map<String, CodeCompleterUtils.OptionsItem> getAllBlockInMaster(Document document, Project editingProject) {
//        FileObject editingDir = editingProject.getProjectDirectory().getFileObject(MyProjectUtils.getProjectWorkbenchDir());
        //  .getFileObject("");
//        List<FileObject> twigFiles = FileSystemUtils.findByMimeType(editingDir, CommonConstants.NB_MIME_TWIG);
        List<FileObject> twigFiles = TwigCache.getTwig();
        Map<String, CodeCompleterUtils.OptionsItem> result = new HashMap<String, CodeCompleterUtils.OptionsItem>();

        String fileName = null;
        try {
            Matcher haveExtendsMatcher = haveExtendsPattern.matcher(document.getText(0, document.getLength()));
            if (haveExtendsMatcher.find()) {
                fileName = haveExtendsMatcher.group("fileName");
            }
            if (fileName != null) {
                for (FileObject twigFile : twigFiles) {
                    if (twigFile.getPath().endsWith(fileName)) {
                        String text;
                        text = twigFile.asText();
                        Matcher matcher = blockCommentPattern.matcher(text);
                        while (matcher.find()) {
                            String name = matcher.group("name");
                            String comments = matcher.group("comments");
                            if (comments == null || comments.trim().equals("")) {
                                comments = "-- No comments found for the block --";
                            } else {
                                comments = comments.substring(3, comments.length() - 3);
                            }
                            result.put(
                                name,
                                new CodeCompleterUtils.OptionsItem(
                                    name,
                                    comments + CodeCompleterUtils.getFileObjectInfo(editingProject, twigFile)
                                ));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
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
                String PREFIX_BLOCK = "b";

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
                if (filter == null || !filter.startsWith(PREFIX_BLOCK)) {
                    completionResultSet.finish();
                    return;
                }

                Map<String, CodeCompleterUtils.OptionsItem> options = getAllBlockInMaster(document, CodeCompleterUtils.getEditingProject());
                for (Map.Entry<String, CodeCompleterUtils.OptionsItem> option : options.entrySet()) {
                    if ((PREFIX_BLOCK + "lock " + option.getKey()).startsWith(filter)) {
                        completionResultSet.addItem(new GeneralCompletionItem(
                                (docText.contains(PREFIX_BLOCK + "lock " + option.getKey())),
                                "Twig block",
                                PREFIX_BLOCK + "lock " + option.getKey(),
                                WRAPPER + PREFIX_BLOCK + "lock " + option.getKey() + " %}\r\n\r\n{% endblock " + option.getKey() + " %}",
                                option.getValue(),
                                startOffset,
                                caretOffset
                        ));
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
