package com.eye.protect.receiver

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.eye.protect.R

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, R.string.enable_device_admin, Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "设备管理权限已关闭，无法锁屏", Toast.LENGTH_LONG).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceAdminReceiver::class.java)
        }

        fun isActive(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                dpm.isAdminActive(getComponentName(context))
            } catch (_: Exception) {
                false
            }
        }

        fun lockNow(context: Context) {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                dpm.lockNow()
                android.util.Log.d("DeviceAdmin", "lockNow() called successfully")
            } catch (e: SecurityException) {
                android.util.Log.e("DeviceAdmin", "lockNow() failed - no admin permission", e)
            } catch (e: Exception) {
                android.util.Log.e("DeviceAdmin", "lockNow() failed", e)
            }
        }
    }
}
