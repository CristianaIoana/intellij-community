// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy.type;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.ide.hierarchy.*;
import com.intellij.ide.hierarchy.newAPI.HierarchyScopeType;
import com.intellij.ide.hierarchy.newAPI.TypeHierarchyBrowserBase;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class JavaTypeHierarchyProvider implements HierarchyProvider {
  private static final Logger LOG = Logger.getInstance(JavaTypeHierarchyProvider.class);
  @Override
  public PsiElement getTarget(@NotNull final DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return null;

    final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (LOG.isDebugEnabled()) {
      LOG.debug("editor " + editor);
    }
    if (editor != null) {
      final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return null;

      final PsiElement targetElement = TargetElementUtil.findTargetElement(editor, TargetElementUtil.ELEMENT_NAME_ACCEPTED |
                                                                                   TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED |
                                                                                   TargetElementUtil.LOOKUP_ITEM_ACCEPTED);
      if (LOG.isDebugEnabled()) {
        LOG.debug("target element " + targetElement);
      }
      if (targetElement instanceof PsiClass) {
        return targetElement;
      }

      final int offset = editor.getCaretModel().getOffset();
      PsiElement element = file.findElementAt(offset);
      while (element != null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("context element " + element);
        }
        if (element instanceof PsiFile) {
          if (!(element instanceof PsiClassOwner)) return null;
          final PsiClass[] classes = ((PsiClassOwner)element).getClasses();
          return classes.length == 1 ? classes[0] : null;
        }
        if (element instanceof PsiClass && !(element instanceof PsiAnonymousClass) && !(element instanceof PsiSyntheticClass)) {
          return element;
        }
        element = element.getParent();
      }

      return null;
    }
    else {
      final PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
      return element instanceof PsiClass ? (PsiClass)element : null;
    }
  }

  @Override
  @NotNull
  public HierarchyBrowser createHierarchyBrowser(@NotNull PsiElement target) {
    return new TypeHierarchyBrowser(target.getProject(), (PsiClass) target);
  }

  @Override
  public void browserActivated(@NotNull final HierarchyBrowser hierarchyBrowser) {
    final TypeHierarchyBrowser browser = (TypeHierarchyBrowser)hierarchyBrowser;
    final HierarchyScopeType typeName =
      browser.isInterface() ? TypeHierarchyBrowserBase.getSubtypesHierarchyType() : TypeHierarchyBrowserBase.getTypeHierarchyType();
    browser.changeView(typeName);
  }
}
