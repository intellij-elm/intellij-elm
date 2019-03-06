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


/**
 * Show a balloon notification along with action(s). The notification will be automatically dismissed
 * when an action is invoked.
 *
 * @param content The main content to be shown in the notification
 * @param type The notification type
 * @param actions Optional list of actions to be included in the notification
 */
fun Project.showBalloon(
        content: String,
        type: NotificationType,
        vararg actions: Pair<String, (() -> Unit)>
) {
    val notification = pluginNotifications.createNotification(content, type)
    for ((actionTitle, actionCallback) in actions) {
        notification.addAction(
                NotificationAction.create(actionTitle)
                { _, notif ->
                    notif.hideBalloon()
                    actionCallback()
                }
        )
    }
    Notifications.Bus.notify(notification, this)
}
