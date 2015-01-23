/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin.Utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author peter.ho
 */
public class FileSystemUtils {

    public static List<FileObject> findByMimeType(FileObject root, String mime) {
        if (root == null || !root.isFolder()) {
            throw new IllegalArgumentException("Not a folder");
        }
        List<FileObject> result = new ArrayList<FileObject>();
        Enumeration<? extends FileObject> children = root.getChildren(true);
        while (children.hasMoreElements()) {
            FileObject file = children.nextElement();
            if (mime.equals(file.getMIMEType())) {
                result.add(file);
            }
        }
        return result;
    }

    /**
     * Find all groups staring with '/&ast;&ast;' and ending with &ast;/
     */
    public static List<String> getDocComments(String s) {
        Pattern pattern = Pattern.compile("/\\*\\*.*?\\*/", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(s);
        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
    }
}
