/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.php.twigplugin.TwigCache;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;

/**
 *
 * @author peter.ho
 */
public class HyperlinkProviderUtils {

    public static Project getEditingProject() {
        FileObject editFile = Utilities.actionsGlobalContext().lookup(FileObject.class);
        Project owner = FileOwnerQuery.getOwner(editFile);
        return owner;
    }

    public static String getTwigCommonFile(String twigName) {
        if (twigName != null && twigName.trim().length() > 0) {
//            List<FileObject> twigFiles = MyProjectUtils.findByMimeType(getEditingProject(), CommonConstants.NB_MIME_TWIG);
            List<FileObject> twigFiles = TwigCache.getTwig();
            for (FileObject twigFile : twigFiles) {
                if (twigFile.getPath().endsWith(twigName + ".twig")) {
                    return twigFile.getPath();
                }
                if (twigFile.getPath().endsWith(twigName)) {
                    return twigFile.getPath();
                }
            }
        }
        return null;
    }

    public static int indexOf(Pattern pattern, String s) {
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }

}
