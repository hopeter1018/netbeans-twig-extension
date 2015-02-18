/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.*;
import org.netbeans.api.project.Project;
import static org.netbeans.modules.php.twigplugin.Utils.HyperlinkProviderUtils.getEditingProject;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author peter.ho
 */
public class ProjectUtils {
    
    protected static String getComposerJsonProperty(String... propertyNames)
    {
        JSONObject composerObj;
        String result = null;
        try {
            FileObject editingDir = getEditingProject().getProjectDirectory();
            String jsonStr = editingDir.getFileObject("composer.json").asText();
            Object composer = JSONValue.parse(jsonStr);
            composerObj = (JSONObject) composer;

            for (String propertyName : propertyNames) {
                if (composerObj.get(propertyName) instanceof JSONObject) {
                    composerObj = (JSONObject) composerObj.get(propertyName);
                } else if (composerObj.get(propertyName) instanceof String) {
                    result = (String) composerObj.get(propertyName);
                }
            }
            //  String pageName = obj.getJSONObject("pageInfo").getString("pageName");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return result;
    }

    public static String getComposerVendorDir()
    {
        return getComposerJsonProperty("config", "vendor-dir");
    }

    public static String getProjectWorkbenchDir()
    {
        return getComposerJsonProperty("config", "vendor-dir").replace("/vendor", "/workbench");
    }

    public static String getMyVendorDir()
    {
        return getComposerJsonProperty("config", "vendor-dir") + "/hopeter1018";
    }

    public static String getZmsVendorDir()
    {
        return getComposerJsonProperty("config", "vendor-dir") + "/zms5";
    }

    public static List<FileObject> findByMimeType(Project editingProject, String mime)
    {
        List<FileObject> result = new ArrayList();
        List<FileObject> paths = getEditingPaths(editingProject);
        for (FileObject path : paths) {
            result.addAll(FileSystemUtils.findByMimeType(path, mime));
        }
        return result;
    }

    public static List<FileObject> getEditingPaths(Project editingProject)
    {
        List<FileObject> result = new ArrayList();
        result.add(editingProject.getProjectDirectory().getFileObject(ProjectUtils.getProjectWorkbenchDir()));
        result.add(editingProject.getProjectDirectory().getFileObject(ProjectUtils.getMyVendorDir()));
        result.add(editingProject.getProjectDirectory().getFileObject(ProjectUtils.getZmsVendorDir()));
        return result;
    }

}