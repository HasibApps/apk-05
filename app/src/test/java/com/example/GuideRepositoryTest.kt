package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DefaultGuideData
import com.example.data.GuideRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuideRepositoryTest {

    @Test
    fun `default database contains all 7 subjects with 1st and 2nd paper`() {
        val db = DefaultGuideData.create()
        assertEquals(7, db.subjects.size)
        DefaultGuideData.SUBJECT_ORDER.forEach { subject ->
            val data = db.subjects[subject]
            assertTrue("Missing subject $subject", data != null)
            assertTrue("Missing 1st paper mainBook for $subject", data!!.first.mainBook.isNotEmpty())
            assertTrue("Missing 1st paper testPaper for $subject", data.first.testPaper.isNotEmpty())
            assertTrue("Missing 2nd paper mainBook for $subject", data.second.mainBook.isNotEmpty())
            assertTrue("Missing 2nd paper testPaper for $subject", data.second.testPaper.isNotEmpty())
        }
    }

    @Test
    fun `export and import json preserves all subjects`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GuideRepository.getInstance(context)
        val json = repo.exportJsonString()
        assertTrue(json.contains("বাংলা"))
        assertTrue(json.contains("উচ্চতর গণিত"))
        assertTrue(repo.importJsonString(json))
        assertEquals(7, repo.getDatabase().subjects.size)
    }

    @Test
    fun `google drive preview url resolves to direct download url`() {
        val preview = "https://drive.google.com/file/d/1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov/preview?usp=drivesdk"
        val resolved = GuideRepository.resolveDirectDownloadUrl(preview)
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1u0EbFJbiK3woAxjPeNrB0PNG8STnmbov",
            resolved
        )
    }
}
