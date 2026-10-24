import re

with open("app/src/main/java/com/example/data/VaccineRepository.kt", "r") as f:
    content = f.read()

target = """    suspend fun registerChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dob: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean
    ): Long = withContext(Dispatchers.IO) {
        // Generate independent unique clinical ID, e.g., IRQ-26-XXXX
        val randomSuffix = (10000..99999).random()
        val uniqueId = "IRQ-26-$randomSuffix"

        val child = Child(
            uniqueId = uniqueId,
            fullName = fullName,
            motherName = motherName,
            healthCenter = healthCenter.ifBlank { "طاق ميكائيل" },
            address = address,
            phone = phone,
            dob = dob,
            vaccinationNumber = vaccinationNumber,
            familyNumber = familyNumber,
            headOfHousehold = headOfHousehold,
            isAdultTd = isAdultTd
        )"""

replacement = """    suspend fun registerChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dob: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean,
        gender: String = "ذكر",
        photoUri: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // Generate independent unique clinical ID, e.g., IRQ-26-XXXX
        val randomSuffix = (10000..99999).random()
        val uniqueId = "IRQ-26-$randomSuffix"

        val child = Child(
            uniqueId = uniqueId,
            fullName = fullName,
            motherName = motherName,
            healthCenter = healthCenter.ifBlank { "طاق ميكائيل" },
            address = address,
            phone = phone,
            dob = dob,
            vaccinationNumber = vaccinationNumber,
            familyNumber = familyNumber,
            headOfHousehold = headOfHousehold,
            isAdultTd = isAdultTd,
            gender = gender,
            photoUri = photoUri
        )"""

if target in content:
    content = content.replace(target, replacement)
    print("Repo register updated.")
else:
    print("Repo register NOT updated.")

with open("app/src/main/java/com/example/data/VaccineRepository.kt", "w") as f:
    f.write(content)
