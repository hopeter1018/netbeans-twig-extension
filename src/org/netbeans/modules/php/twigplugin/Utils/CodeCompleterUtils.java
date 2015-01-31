package org.netbeans.modules.php.twigplugin.Utils;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.OffsetRange;
//import org.netbeans.modules.php.zms5.UserAccessControl.AnnotationCompleter;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;
import org.netbeans.modules.php.api.annotation.util.AnnotationUtils;
import org.netbeans.modules.php.api.editor.EditorSupport;
import org.netbeans.modules.php.api.editor.PhpClass;
import org.openide.util.Lookup;

final public class CodeCompleterUtils {

    final static Pattern namespacePattern = Pattern.compile("namespace (.*);");
                
    public static class OptionsItem
    {
        public final String name;
        public final String template;

        public OptionsItem(String name, String template) {
            this.name = name;
            this.template = template;
        }
    }

    public static class PhpClassFile
    {
        public final String name;
        public final PhpClass phpClass;
        public final FileObject fileObject;
        public final String annoParam;
        public String namespaceString;

        public PhpClassFile(String name, PhpClass phpClass, FileObject fileObject) {
            this.name = name;
            this.phpClass = phpClass;
            this.fileObject = fileObject;
            this.namespaceString = null;

            String mydata;
            try {
                mydata = fileObject.asText();
                Matcher matcher = namespacePattern.matcher(mydata);
                if (matcher.find())
                {
                    this.namespaceString = matcher.group(1);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            String paraString = "";
            Collection<PhpClass.Field> fields = this.phpClass.getFields();
            if (fields != null && fields.size() > 0) {
                for (PhpClass.Field field : fields) {
                    String type = "";
                    try {
                        type = CodeCompleterUtils.getSingleAnnotationValueFromDocBlock(
                                CodeCompleterUtils.getLastDocBlockByOffset(this.fileObject, field.getOffset()),
                                "var"
                        );
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    paraString += ", " + field.getName().substring(1) + "=\"${" + type + "}\"";
                }
                this.annoParam = paraString.substring(2);
            } else {
                this.annoParam = "";
            }
        }
    };

    public static String getFileObjectInfo(FileObject fileObject)
    {
        return "<hr />File Location: " + fileObject.getPath() + "<br /><a href=\"" + fileObject.getPath() + "\"></a><br />";
    }

    public static boolean isPhpClass(Document document)
    {
        boolean isPhpClass = false;
        if (document != null) {
            String text;
            try {
                text = document.getText(0, document.getLength());
                if (Pattern.compile("namespace ([^ ;]+);").matcher(text).find())
                {
                    Matcher matcher = Pattern.compile("class ([^ {]+)").matcher(text);
                    if (matcher.find())
                    {
                        if (matcher.group(1) != null) {
                            isPhpClass = true;
                        }
                    }
                }
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return isPhpClass;
    }

//    public static boolean isPhpClassExtends(Document document, String superClassName)
//    {
//        boolean isPhpClassExtends = false;
//        if (isPhpClass(document)) {
//            
//        }
//        return isPhpClassExtends;
//    }

    public static Project getEditingProject() {
        FileObject editFile = Utilities.actionsGlobalContext().lookup(FileObject.class);
        Project owner = FileOwnerQuery.getOwner(editFile);
        return owner;
    }

    public static String getSingleAnnotationValueFromDocBlock(String text, String annotationName)
    {
        String type = null;
        HashSet<String> annoVar = new HashSet<String>();
        annoVar.addAll(Arrays.asList(annotationName));
        Map<OffsetRange, String> newResult = AnnotationUtils.extractInlineAnnotations(text, annoVar);
        for (Map.Entry<OffsetRange, String> result : newResult.entrySet()) {
            String line = text.substring(result.getKey().getEnd() + 1);
            type = line.substring(0, line.indexOf(" "));
        }
        return type;
    }

    public static Map<String, PhpClassFile> getAllPhpWithAnnotations(Project editingProject, String... toFindAnnotations) {
        Map<String, PhpClassFile> result = new HashMap<String, PhpClassFile>();
        List<FileObject> phpFiles = ProjectUtils.findByMimeType(editingProject, "text/x-php5");

        Set<String> annoSet = new java.util.HashSet<String>();
        annoSet.addAll(Arrays.asList(toFindAnnotations));

        EditorSupport editorSupport = Lookup.getDefault().lookup(EditorSupport.class);
        for (FileObject phpFile : phpFiles) {
            try {
                Map<OffsetRange, String> newResult = AnnotationUtils.extractInlineAnnotations(phpFile.asText(), annoSet);
                if (!newResult.isEmpty()) {
                    Collection<PhpClass> classes = editorSupport.getClasses(phpFile);
                    for (PhpClass phpClass : classes) {
                        result.put(phpClass.getName(), new PhpClassFile(phpClass.getName(), phpClass, phpFile));
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return result;
    }

    public static int getLastIndexOfDoc(StyledDocument doc, int offset, String findText) throws BadLocationException
    {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + findText.length() < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, findText.length()).equals(findText)) {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + findText.length())
                        + ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    public static String getLastDocBlockByOffset(FileObject fileObject, int offset) throws IOException
    {
        if (fileObject != null) {
            String text = fileObject.asText().substring(0, offset);
            return text.substring(text.lastIndexOf("/*"), text.lastIndexOf("*/"));
        }
        return null;
    }

    public static void getListOfResource() {
//        System.out.println("###################################################################");
////        System.out.println(this.getClass().toString());
//        URL url = AnnotationCompleter.class.getResource("AnnotationCompleter.class");
//        String scheme = url.getProtocol();
//        if (!"jar".equals(scheme)) {
//            throw new IllegalArgumentException("Unsupported scheme: " + scheme);
//        }
//        JarURLConnection con;
//        try {
//            con = (JarURLConnection) url.openConnection();
//            JarFile archive = con.getJarFile();
//            /* Search for the entries you care about. */
//            Enumeration<JarEntry> entries = archive.entries();
//            while (entries.hasMoreElements()) {
//                JarEntry entry = entries.nextElement();
//                System.out.println(entry.getName());
//            }
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
//        }
    }

}
