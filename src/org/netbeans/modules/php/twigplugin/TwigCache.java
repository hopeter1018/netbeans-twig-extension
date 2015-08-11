/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.twigplugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.php.twigplugin.Utils.CommonConstants;
import org.netbeans.modules.php.twigplugin.Utils.HyperlinkProviderUtils;
import org.netbeans.modules.php.twigplugin.Utils.MyProjectUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author peter.ho
 */
public class TwigCache {

    private final Map<String, List<FileObject>> cachedPhp = new HashMap<String, List<FileObject>>();
    private final Map<String, List<FileObject>> cachedTwig = new HashMap<String, List<FileObject>>();

    private static boolean inited = false;
    public TwigCache() {
        if (inited == false) {
            inited = true;
            FileUtil.addFileChangeListener(new UpdateCacheWhenFileChangedListener());

            Project editingProject = HyperlinkProviderUtils.getEditingProject();
            ProjectInformation info = ProjectUtils.getInformation(editingProject);
            cachedPhp.put(info.getName(), readProjectPhpFromFiles(editingProject));
            cachedTwig.put(info.getName(), readProjectTwigFromFiles(editingProject));
        }
StatusDisplayer.getDefault().setStatusText("using TwigCache");
    }

    private class UpdateCacheWhenFileChangedListener extends FileChangeAdapter {

        @Override
        public void fileDataCreated(FileEvent fe) {
            updateCache(fe);
        }

        @Override
        public void fileChanged(FileEvent fe) {
            updateCache(fe);
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            updateCache(fe);
        }

        private void updateCache(FileEvent fe) {
            FileObject file = fe.getFile();
            boolean isPhp = CommonConstants.NB_MIME_PHP.equals(file.getMIMEType());
            boolean isTwig = CommonConstants.NB_MIME_TWIG.equals(file.getMIMEType());
            if (! (isPhp|| isTwig)) {
                return;
            }
            Project project = FileOwnerQuery.getOwner(file);
            if (project == null) {
                return;
            }
            ProjectInformation info = ProjectUtils.getInformation(project);
            if (isPhp) {
                cachedPhp.put(info.getName(), readProjectPhpFromFiles(project));
            } else {
                cachedTwig.put(info.getName(), readProjectTwigFromFiles(project));
            }
        }
    }

    public static List<FileObject> getPhp() {
        Project project = HyperlinkProviderUtils.getEditingProject();
        ProjectInformation info = ProjectUtils.getInformation(project);
        String projectName = info.getName();
        TwigCache inst = new TwigCache();
        if (!inst.cachedPhp.containsKey(projectName)) {
            inst.cachedPhp.put(projectName, readProjectPhpFromFiles(project));
        }
        return inst.cachedPhp.get(projectName);
    }

    private static List<FileObject> readProjectPhpFromFiles(Project project) {
        return MyProjectUtils.findByMimeType(project, CommonConstants.NB_MIME_PHP);
    }

    public static List<FileObject> getTwig() {
        Project project = HyperlinkProviderUtils.getEditingProject();
        ProjectInformation info = ProjectUtils.getInformation(project);
        String projectName = info.getName();
        TwigCache inst = new TwigCache();
        if (!inst.cachedTwig.containsKey(projectName)) {
            inst.cachedTwig.put(projectName, readProjectTwigFromFiles(project));
        }
        return inst.cachedTwig.get(projectName);
    }

    private static List<FileObject> readProjectTwigFromFiles(Project project) {
        return MyProjectUtils.findByMimeType(project, CommonConstants.NB_MIME_TWIG);
    }

}
