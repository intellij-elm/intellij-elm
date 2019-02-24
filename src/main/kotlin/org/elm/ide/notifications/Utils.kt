/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 *
 * Originally from intellij-rust
 */

package org.elm.ide.notifications

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project

private val pluginNotifications = NotificationGroup.balloonGroup("Elm Plugin")

fun Project.showBalloon(content: String, type: NotificationType, action: NotificationAction? = null) {
    val notification = pluginNotifications.createNotification(content, type)
    if (action != null) {
        notification.addAction(action)
    }
    Notifications.Bus.notify(notification, this)
}
