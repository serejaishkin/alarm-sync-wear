package com.wakesync.app

import android.app.backup.BackupAgentHelper
import com.wakesync.app.worker.BootRescheduleWorker

class RestoreAlarmAgent : BackupAgentHelper() {

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        BootRescheduleWorker.enqueue(
            context = applicationContext,
            sourceAction = "RESTORE_FINISHED",
            forceRecalculate = true
        )
    }
}
