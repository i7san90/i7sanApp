import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

target = """                    Spacer(modifier = Modifier.height(4.dp))

                    // Second: Child's Date of Birth / Age"""

replacement = """                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "صورة الطفل والجنس",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(40.dp)).clickable {
                                galleryLauncher.launch("image/*")
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                coil.compose.AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(40.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                val defaultImg = if (gender == "أنثى") androidx.compose.material.icons.Icons.Default.Face else androidx.compose.material.icons.Icons.Default.Person
                                Icon(defaultImg, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                                Text("اختر من المعرض")
                            }
                            Button(onClick = {
                                val tempFile = java.io.File.createTempFile("child_photo_", ".jpg", context.cacheDir)
                                tempPhotoUri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
                                cameraLauncher.launch(tempPhotoUri!!)
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text("التقط صورة")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "ذكر" }) {
                            RadioButton(selected = gender == "ذكر", onClick = { gender = "ذكر" })
                            Text("ذكر", modifier = Modifier.padding(start = 4.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = "أنثى" }) {
                            RadioButton(selected = gender == "أنثى", onClick = { gender = "أنثى" })
                            Text("أنثى", modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Second: Child's Date of Birth / Age"""

if target in content:
    content = content.replace(target, replacement)
    print("UI added.")
else:
    print("UI NOT added.")

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
