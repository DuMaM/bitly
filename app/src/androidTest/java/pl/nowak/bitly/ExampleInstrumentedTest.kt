package pl.nowak.bitly

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.nowak.bitly.database.LeadDatabase
import pl.nowak.bitly.database.LeadDatabaseDao
import pl.nowak.bitly.database.LeadEntry
import java.io.IOException


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("pl.nowak.bitly", appContext.packageName)
    }
}


@RunWith(AndroidJUnit4::class)
class SleepDatabaseTest {

    private lateinit var leadDao: LeadDatabaseDao
    private lateinit var db: LeadDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Using an in-memory database because the information stored here disappears when the
        // process is killed.
        db = Room.inMemoryDatabaseBuilder(context, LeadDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        leadDao = db.leadDatabaseDao
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetEntry() {
        val new_entry = LeadEntry(x = 1.1f, y = 2.2f, lead = LeadName.LeadAVL)
        leadDao.insert(new_entry)
        val entry = leadDao.get(0)
        assertEquals(entry.lead?.name, LeadName.LeadAVL.name)
    }
}