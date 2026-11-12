package com.hasib.guide

import org.json.*
import java.io.File
import java.security.MessageDigest

object Store {
    const val ADMIN_HASH = "1626fb4b324c14859bc0bd6a55ce3fd09a59e7f463828548e7c1c82c7d09adbc" // SHA-256 (পাসওয়ার্ড কোডে লেখা নেই)
    val subjects = listOf("বাংলা", "ইংরেজি", "আইসিটি", "পদার্থবিজ্ঞান", "রসায়ন", "জীববিজ্ঞান", "উচ্চতর গণিত")
    lateinit var root: JSONObject
    private lateinit var file: File

    fun init(dir: File) {
        file = File(dir, "data.json")
        root = try { JSONObject(file.readText()) } catch (e: Exception) { default() }
        if (!file.exists()) save()
    }
    private fun item(t: String, folder: Boolean) = JSONObject().put("t", t).put("f", folder).put("l", "").put("c", JSONArray())
    private fun default(): JSONObject {
        val r = JSONObject()
        for (s in subjects) {
            val sub = JSONObject()
            for (p in listOf("first", "second")) sub.put(p, JSONObject()
                .put("main", JSONArray().put(item("সকল প্রকাশনির মূল বই", true)).put(item("সকল অধ্যায় MCQ", false)).put(item("বেসিক ধারনা (Royal)", false)))
                .put("test", JSONArray().put(item("All tast paper", true))))
            r.put(s, sub)
        }
        return r
    }
    fun save() = file.writeText(root.toString(2))
    fun list(s: String, p: String, sec: String): JSONArray = root.getJSONObject(s).getJSONObject(p).getJSONArray(sec)
    fun import(text: String) { root = JSONObject(text); save() }
    fun sha(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
