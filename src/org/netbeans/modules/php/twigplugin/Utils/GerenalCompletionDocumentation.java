/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin.Utils;

import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Action;
import org.netbeans.spi.editor.completion.CompletionDocumentation;
import org.openide.util.Exceptions;

/**
 *
 * @author peter.ho
 */
public class GerenalCompletionDocumentation implements CompletionDocumentation {

    private final GeneralCompletionItem item;

    public GerenalCompletionDocumentation(GeneralCompletionItem item) {
        this.item = item;
    }

    @Override
    public String getText() {
        return this.item.getOptionsItem().template;
    }

    @Override
    public URL getURL() {
//        try {
//            return new URL("http://codex.wordpress.org/Plugin_API/Action_Reference"); // NOI18N
//        } catch (MalformedURLException ex) {
//            Exceptions.printStackTrace(ex);
//        }
        return null;
    }

    @Override
    public CompletionDocumentation resolveLink(String string) {
        return null;
    }

    @Override
    public Action getGotoSourceAction() {
        return null;
    }

}
