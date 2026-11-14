package com.example.pdf

import android.content.Context

/**
 * পাসওয়ার্ড-সুরক্ষিত PDF এর আসল পাসওয়ার্ড একবার দিলে অ্যাপে (প্রাইভেট স্টোরেজে) সেভ থাকে,
 * পরের বার একই PDF খুললে নিজে থেকেই বসে যায়।
 * কী = PDF ফাইলের id (ডাউনলোড করা ফাইলের নাম)।
 */
class PdfPasswordStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("pdf_passwords", Context.MODE_PRIVATE)

    fun get(pdfId: String): String? = prefs.getString(pdfId, null)

    fun save(pdfId: String, password: String) {
        prefs.edit().putString(pdfId, password).apply()
    }

    fun remove(pdfId: String) {
        prefs.edit().remove(pdfId).apply()
    }
}
