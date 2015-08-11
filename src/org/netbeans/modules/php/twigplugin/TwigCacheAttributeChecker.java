/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin;

import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 *
 * @author peter.ho
 */
public abstract class TwigCacheAttributeChecker {
    
    protected final List<FileObject> result = new ArrayList<FileObject>();

    protected abstract boolean isMatched(FileObject fileObject);

}
