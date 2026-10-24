import re

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "r") as f:
    content = f.read()

# Replace the buttons in ChildDetailsScreen
old_buttons_block = """                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val safeName = currentChild.fullName.replace(" ", "_")
                                    saveChildPdfLauncher.launch("سجل_تلقيح_${safeName}.pdf")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنزيل كارت لقاح (PDF)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    VaccineReportExporter.printChildReportAsHtml(
                                        context,
                                        currentChild,
                                        vaccines,
                                        "المركز الصحي المعتمد" // You could pass actual center name here if you want
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Print, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة سجل اللقاحات", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }"""

new_buttons_block = """                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // Save PDF Button
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val safeName = currentChild.fullName.replace(" ", "_")
                                    saveChildPdfLauncher.launch("سجل_تلقيح_${safeName}.pdf")
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "حفظ كارت", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Print Button
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val centerName = context.getSharedPreferences("vaccine_prefs", android.content.Context.MODE_PRIVATE).getString("health_center_name", "طاق ميكائيل") ?: "طاق ميكائيل"
                                    VaccineReportExporter.printChildReportAsHtml(context, currentChild, vaccines, centerName)
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "طباعة الكارت", tint = MaterialTheme.colorScheme.secondary)
                            }

                            // Share Button
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, "سجل تطعيم الطفل ${currentChild.fullName} متاح الآن في نظام اللقاحات.")
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                shape = androidx.compose.foundation.shape.CircleShape,
                                contentPadding = PaddingValues(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF009688)),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF009688))
                            }
                        }"""

content = content.replace(old_buttons_block, new_buttons_block)

# Fix missing Icons.Default.Download if not imported (Compose defaults has it)
# We can also add labels under the icons if we want, but circles are elegant.

with open("app/src/main/java/com/example/ui/VaccineTrackerApp.kt", "w") as f:
    f.write(content)
print("Replaced buttons.")
