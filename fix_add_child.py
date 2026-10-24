import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """    var headOfHousehold by remember { mutableStateOf("") }
    val isAdultTd = false"""

replacement = """    var headOfHousehold by remember { mutableStateOf("") }
    val isAdultTd = false
    
    var gender by remember { mutableStateOf("ذكر") }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { photoUri = it }
    }
    
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = tempPhotoUri
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    print("Launcher added.")
else:
    print("Launcher NOT added.")

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
