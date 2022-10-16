package pl.nowak.bitly.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.nowak.bitly.LeadName
import pl.nowak.bitly.database.LeadDatabase
import pl.nowak.bitly.database.LeadEntry


class EcgDataRepository(private val database: LeadDatabase) {
    suspend fun refreshData() {
        withContext(Dispatchers.IO) {
            // val getBleData = BLe...
            val vars: Array<LeadEntry> = emptyArray()
            database.leadDao.insert(*vars)
        }
    }

    private val leadDataSize = 60

    val leadV1: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV1.ordinal, leadDataSize)
    val leadV2: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV2.ordinal, leadDataSize)
    val leadV3: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV3.ordinal, leadDataSize)
    val leadV4: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV4.ordinal, leadDataSize)
    val leadV5: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV5.ordinal, leadDataSize)
    val leadV6: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadV6.ordinal, leadDataSize)
    val leadI: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadI.ordinal, leadDataSize)
    val leadII: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadII.ordinal, leadDataSize)
    val leadIII: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadIII.ordinal, leadDataSize)
    val leadAVL: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVL.ordinal, leadDataSize)
    val leadAVR: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVR.ordinal, leadDataSize)
    val leadAVF: LiveData<List<LeadEntry>> = database.leadDao.getLead(LeadName.LeadAVF.ordinal, leadDataSize)
}