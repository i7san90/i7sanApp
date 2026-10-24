package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * مدير عمليات Firestore
 * يوفر الدوال الأساسية لإضافة وقراءة ملفات الأطفال وجداول اللقاحات الخاصة بهم
 */
class FirestoreRepository {
    // تهيئة Firestore
    private val db by lazy { FirebaseFirestore.getInstance() }

    // مراجع الـ Collections
    private val familiesCollection by lazy { db.collection("families") }
    private val childrenCollection by lazy { db.collection("children") }

    /**
     * إضافة أو تحديث بيانات الأسرة
     */
    suspend fun saveFamily(family: FamilyDocument): String {
        val docRef = if (family.id.isEmpty()) {
            familiesCollection.document() // إنشاء ID جديد
        } else {
            familiesCollection.document(family.id)
        }
        
        docRef.set(family).await()
        return docRef.id
    }

    /**
     * إضافة طفل جديد مع جدول لقاحاته الافتراضي
     */
    suspend fun registerChildWithVaccines(child: ChildDocument): String {
        val docRef = if (child.id.isEmpty()) {
            childrenCollection.document()
        } else {
            childrenCollection.document(child.id)
        }

        // حفظ بيانات الطفل
        docRef.set(child).await()

        // إضافة جدول اللقاحات القياسي (النظام العراقي) للطفل كـ Sub-collection
        val vaccinesCollection = docRef.collection("vaccinations")
        
        // استخدام Batch writing لتحسين الأداء عند إضافة اللقاحات المتعددة
        val batch = db.batch()
        
        IraqiVaccineSchedule.schedule.forEach { vaccine ->
            val vaccineRef = vaccinesCollection.document(vaccine.vaccineKey)
            batch.set(vaccineRef, vaccine)
        }
        
        batch.commit().await()
        return docRef.id
    }

    /**
     * رفع بيانات الطفل مع سجلات اللقاحات الخاصة به من قاعدة بيانات Room إلى Firestore لضمان النسخ الاحتياطي السحابي
     */
    suspend fun uploadChildWithRecords(childDoc: ChildDocument, vaccineDocs: List<VaccineDocument>): String {
        val docRef = if (childDoc.uniqueId.isNotEmpty()) {
            val query = childrenCollection.whereEqualTo("uniqueId", childDoc.uniqueId).get().await()
            if (!query.isEmpty) {
                query.documents.first().reference
            } else if (childDoc.id.isNotEmpty()) {
                childrenCollection.document(childDoc.id)
            } else {
                childrenCollection.document()
            }
        } else if (childDoc.id.isNotEmpty()) {
            childrenCollection.document(childDoc.id)
        } else {
            childrenCollection.document()
        }

        val childId = docRef.id
        val finalChildDoc = childDoc.copy(id = childId)

        // حفظ بيانات الطفل في Firestore
        docRef.set(finalChildDoc, SetOptions.merge()).await()

        // حفظ سجلات اللقاحات الخاصة بالطفل في الـ Sub-collection
        val vaccinesCollection = docRef.collection("vaccinations")
        val batch = db.batch()

        vaccineDocs.forEach { vaccine ->
            val vaccineRef = vaccinesCollection.document(vaccine.vaccineKey)
            batch.set(vaccineRef, vaccine, SetOptions.merge())
        }

        batch.commit().await()
        return childId
    }

    /**
     * تحديث حالة أخذ اللقاح
     */
    suspend fun updateVaccineStatus(
        childId: String, 
        vaccineKey: String, 
        isTaken: Boolean, 
        takenDate: String?,
        administeredBy: String? = null
    ) {
        val updates = mapOf(
            "isTaken" to isTaken,
            "takenDate" to takenDate,
            "administeredBy" to administeredBy,
            "updatedAt" to System.currentTimeMillis()
        )
        
        childrenCollection.document(childId)
            .collection("vaccinations")
            .document(vaccineKey)
            .set(updates, SetOptions.merge())
            .await()
    }

    /**
     * جلب جميع الأطفال
     */
    suspend fun getAllChildren(): List<ChildDocument> {
        val snapshot = childrenCollection.get().await()
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ChildDocument::class.java)?.apply {
                id = doc.id
            }
        }
    }

    /**
     * جلب جدول اللقاحات لطفل معين
     */
    suspend fun getChildVaccines(childId: String): List<VaccineDocument> {
        val snapshot = childrenCollection.document(childId)
            .collection("vaccinations")
            .get()
            .await()
            
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(VaccineDocument::class.java)?.apply {
                id = doc.id
            }
        }.sortedBy { it.dueMonths }
    }
}
