import re

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "r") as f:
    content = f.read()

target = """    fun registerNewChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dobString: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val childId = repository.registerChild(
                fullName = fullName,
                motherName = motherName,
                healthCenter = healthCenter,
                address = address,
                phone = phone,
                dob = dobString,
                vaccinationNumber = vaccinationNumber,
                familyNumber = familyNumber,
                headOfHousehold = headOfHousehold,
                isAdultTd = isAdultTd
            )"""

replacement = """    fun registerNewChild(
        fullName: String,
        motherName: String,
        healthCenter: String,
        address: String,
        phone: String,
        dobString: String,
        vaccinationNumber: String,
        familyNumber: String,
        headOfHousehold: String,
        isAdultTd: Boolean,
        gender: String = "ذكر",
        photoUri: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val childId = repository.registerChild(
                fullName = fullName,
                motherName = motherName,
                healthCenter = healthCenter,
                address = address,
                phone = phone,
                dob = dobString,
                vaccinationNumber = vaccinationNumber,
                familyNumber = familyNumber,
                headOfHousehold = headOfHousehold,
                isAdultTd = isAdultTd,
                gender = gender,
                photoUri = photoUri
            )"""

if target in content:
    content = content.replace(target, replacement)
    print("ViewModel register updated.")
else:
    print("ViewModel register NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineViewModel.kt", "w") as f:
    f.write(content)
