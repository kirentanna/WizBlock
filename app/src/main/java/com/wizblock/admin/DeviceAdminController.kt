package com.wizblock.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.wizblock.R

class DeviceAdminController(
    context: Context
) {
    private val appContext = context.applicationContext
    private val devicePolicyManager =
        appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent = ComponentName(appContext, WizBlockDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean {
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    fun createEnableIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                appContext.getString(R.string.device_admin_enable_explanation)
            )
    }

    fun removeActiveAdminIfAllowed(): Boolean {
        return runCatching {
            if (isAdminActive()) {
                devicePolicyManager.removeActiveAdmin(adminComponent)
            }
            true
        }.getOrDefault(false)
    }
}
