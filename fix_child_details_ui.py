import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {"""

replacement = """                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp).padding(8.dp)) {
                        CircularProgressIndicator(
                            progress = { completionPercent / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Box(
                            modifier = Modifier.size(90.dp).clip(RoundedCornerShape(45.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentChild.photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = android.net.Uri.parse(currentChild.photoUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                val defaultImg = if (currentChild.gender == "أنثى") androidx.compose.material.icons.Icons.Default.Face else androidx.compose.material.icons.Icons.Default.Person
                                Icon(defaultImg, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text(
                        text = "${completionPercent}% مكتمل",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(modifier = Modifier.fillMaxWidth()) {"""

if target in content:
    content = content.replace(target, replacement)
    print("ChildDetails UI updated.")
else:
    print("ChildDetails UI NOT updated.")

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
