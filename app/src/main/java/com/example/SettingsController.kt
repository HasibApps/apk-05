package com.example

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.data.GuideRepository
import com.example.databinding.DialogAdminPasswordBinding
import com.example.databinding.DialogSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsController(
    private val activity: AppCompatActivity,
    private val onDataUpdated: () -> Unit = {}
) {
    private val repository = GuideRepository.getInstance(activity)

    private val exportJsonLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    val json = repository.exportJsonString()
                    activity.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    Toast.makeText(activity, "ডাটা (.json) সফলভাবে ডাউনলোড হয়েছে!", Toast.LENGTH_LONG).show()
                }.onFailure { err ->
                    Toast.makeText(activity, "সেভ করতে সমস্যা: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

    private val importJsonLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                runCatching {
                    val json = activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank() && repository.importJsonString(json)) {
                        Toast.makeText(activity, "নতুন ডাটা (.json) সফলভাবে লোড হয়েছে!", Toast.LENGTH_LONG).show()
                        onDataUpdated()
                    } else {
                        Toast.makeText(activity, "সঠিক JSON ফাইল নির্বাচন করুন।", Toast.LENGTH_LONG).show()
                    }
                }.onFailure { err ->
                    Toast.makeText(activity, "ইমপোর্ট ব্যর্থ: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }

    fun showSettingsSheet() {
        val sheet = BottomSheetDialog(activity)
        val sheetBinding = DialogSettingsBinding.inflate(activity.layoutInflater)
        sheet.setContentView(sheetBinding.root)

        sheetBinding.cardAdminOption.setOnClickListener {
            sheet.dismiss()
            showAdminPasswordDialog()
        }

        sheetBinding.cardExportJson.setOnClickListener {
            sheet.dismiss()
            exportJsonLauncher.launch("hsc_guide_data_2026.json")
        }

        sheetBinding.cardImportJson.setOnClickListener {
            sheet.dismiss()
            importJsonLauncher.launch(arrayOf("application/json", "*/*"))
        }

        sheet.show()
    }

    private fun showAdminPasswordDialog() {
        val dlgBinding = DialogAdminPasswordBinding.inflate(activity.layoutInflater)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(dlgBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.admin_unlock, null)
            .create()

        dialog.setOnShowListener {
            val positiveBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val entered = dlgBinding.etAdminPassword.text?.toString()?.trim().orEmpty()
                if (verifyAdminPasscode(entered)) {
                    dialog.dismiss()
                    activity.startActivity(Intent(activity, AdminPanelActivity::class.java))
                } else {
                    dlgBinding.tilAdminPassword.error = activity.getString(R.string.admin_password_error)
                }
            }
        }
        dialog.show()
    }

    private fun verifyAdminPasscode(input: String): Boolean {
        // Secret admin key verification
        val expected = charArrayOf('h', 'a', 's', 'i', 'b', '9', '9').concatToString()
        return input == expected
    }
}
