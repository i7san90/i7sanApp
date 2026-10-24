import sys

content = open("app/src/main/java/com/example/ui/VaccineViewModel.kt").read()

new_content = """
    private val firestoreRepository = com.example.data.FirestoreRepository()

    suspend fun syncDataToFirestore() {
        val allFamilies = repository.getAllFamiliesSync()
        allFamilies.forEach { family ->
            firestoreRepository.saveFamily(
                com.example.data.FamilyDocument(
                    familyNumber = family.familyNumber,
                    headOfHousehold = family.headOfHousehold,
                    address = family.address,
                    phone = family.phone
                )
            )
        }

        val allChildren = repository.getAllChildrenSync()
        allChildren.forEach { child ->
            firestoreRepository.registerChildWithVaccines(
                com.example.data.ChildDocument(
                    uniqueId = child.uniqueId,
                    fullName = child.fullName,
                    motherName = child.motherName,
                    healthCenter = child.healthCenter,
                    address = child.address,
                    phone = child.phone,
                    dob = child.dob,
                    gender = child.gender,
                    registrationDate = child.registrationDate,
                    vaccinationNumber = child.vaccinationNumber,
                    isAdultTd = child.isAdultTd
                )
            )
        }
    }
"""

if "syncDataToFirestore" not in content:
    content = content.replace(") : AndroidViewModel(application) {", ") : AndroidViewModel(application) {\n" + new_content)
    open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w").write(content)
