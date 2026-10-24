import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """                        viewModel.registerNewChild(
                            fullName = fullName.trim(),
                            motherName = motherName.trim(),
                            healthCenter = if (healthCenter.trim().isEmpty()) defaultCenter else healthCenter.trim(),
                            address = if (address.trim().isEmpty()) "غير محدد" else address.trim(),
                            phone = formattedPhone,
                            dobString = formattedDate,
                            vaccinationNumber = vaccinationNumber.trim(),
                            familyNumber = familyNumber.trim(),
                            headOfHousehold = headOfHousehold.trim(),
                            isAdultTd = isAdultTd,
                            onSuccess = {"""

replacement = """                        viewModel.registerNewChild(
                            fullName = fullName.trim(),
                            motherName = motherName.trim(),
                            healthCenter = if (healthCenter.trim().isEmpty()) defaultCenter else healthCenter.trim(),
                            address = if (address.trim().isEmpty()) "غير محدد" else address.trim(),
                            phone = formattedPhone,
                            dobString = formattedDate,
                            vaccinationNumber = vaccinationNumber.trim(),
                            familyNumber = familyNumber.trim(),
                            headOfHousehold = headOfHousehold.trim(),
                            isAdultTd = isAdultTd,
                            gender = gender,
                            photoUri = photoUri?.toString(),
                            onSuccess = {"""

if target in content:
    content = content.replace(target, replacement)
    print("Method call updated.")
else:
    print("Method call NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
