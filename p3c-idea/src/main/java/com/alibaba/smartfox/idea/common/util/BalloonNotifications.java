/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.smartfox.idea.common.util;

;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;

import java.awt.Component;
import java.net.UnknownHostException;

/**
 * @author caikang
 * @date 2017/05/08
 */
public class BalloonNotifications {
    public static String displayId = "SmartFox Intellij IDEA Balloon Notification";
    public static NotificationGroup balloonGroup = new NotificationGroup(displayId, NotificationDisplayType.BALLOON, true);

    public static String stickyBalloonDisplayId = "SmartFox Intellij IDEA Notification";
    public static NotificationGroup stickyBalloonGroup = new NotificationGroup(stickyBalloonDisplayId, NotificationDisplayType.STICKY_BALLOON, true);
    public static String TITLE = "SmartFox Intellij IDEA Plugin";

    void showInfoDialog(Component component, String title, String message) {
        Messages.showInfoMessage(component, message, title);
    }

    void showErrorDialog(Component component, String title, String errorMessage) {
        Messages.showErrorDialog(component, errorMessage, title);
    }

    void showErrorDialog(Component component, String title, Exception e) {
        if (isOperationCanceled(e)) {
            return;
        }
        Messages.showErrorDialog(component, getErrorTextFromException(e), title);
    }

    public static void showSuccessNotification(String message, Project project,
                                 String title, Boolean sticky) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        if (title == null) {
            title = TITLE;
        }
        if (sticky==null)
        sticky = false;

        showNotification(message, project, title, NotificationType.INFORMATION, null, sticky);
    }

    void showWarnNotification(String message, Project project, String title, Boolean sticky) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        if (title == null) {
            title = TITLE;
        }
        sticky = false;
        showNotification(message, project, title, NotificationType.WARNING, null, sticky);
    }

    void showErrorNotification(String message, Project project,
                               String title, Boolean sticky) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        if (title == null) {
            title = TITLE;
        }
        sticky = false;
        showNotification(message, project, title, NotificationType.ERROR, null, sticky);
    }

    void showSuccessNotification(String message, Project project,
                                 NotificationListener notificationListener, String title, Boolean sticky) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        if (title == null) {
            title = TITLE;
        }
        sticky = false;
        showNotification(message, project, title, NotificationType.INFORMATION, notificationListener, sticky);
    }

    public static void showNotification(String message, Project project,
                          String title,
                          NotificationType notificationType,
                          NotificationListener notificationListener, Boolean sticky) {

        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }
        if (title == null) {
            title = TITLE;
        }
        if (notificationType == null) {
            notificationType = NotificationType.INFORMATION;
        }
        if (sticky==null){
            sticky = false;
        }

        NotificationGroup group = sticky ? stickyBalloonGroup : balloonGroup;
        group.createNotification(title, message, notificationType, notificationListener).notify(project);
    }

    private Boolean isOperationCanceled(Exception e) {
        return e instanceof ProcessCanceledException;
    }

    String getErrorTextFromException(Exception e) {
        if (e instanceof UnknownHostException) {
            return "Unknown host: " + e.getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : "";
    }

    static class LogNotifications {
        NotificationGroup group = new NotificationGroup(BalloonNotifications.displayId, NotificationDisplayType.NONE, true);

        void log(String message, Project project, String title,
                 NotificationType notificationType,
                 NotificationListener notificationListener) {

            if (project == null) {
                project = ProjectManager.getInstance().getDefaultProject();
            }
            if (title == null) {
                title = TITLE;
            }
            if (notificationType == null) {
                notificationType = NotificationType.INFORMATION;
            }
            group.createNotification(title, message, notificationType, notificationListener).notify(project);
        }
    }
}

//    object LogNotifications {
//        val group=NotificationGroup(BalloonNotifications.displayId,NotificationDisplayType.NONE,true)
//
//        fun log(message:String,project:Project?=ProjectManager.getInstance().defaultProject,
//        title:String=BalloonNotifications.TITLE,
//        notificationType:NotificationType=NotificationType.INFORMATION,
//        notificationListener:NotificationListener?=null){
//        group.createNotification(title,message,notificationType,notificationListener).notify(project)
//        }
//        }
