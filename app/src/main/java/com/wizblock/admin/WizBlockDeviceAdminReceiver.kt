package com.wizblock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.wizblock.R

class WizBlockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.device_admin_enabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.device_admin_disabled, Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return context.getString(R.string.device_admin_disable_warning)
    }
}
