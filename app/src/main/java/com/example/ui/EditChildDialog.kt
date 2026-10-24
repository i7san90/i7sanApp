package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.Child

@Composable
fun EditChildDialog(
    child: Child,
    onDismiss: () -> Unit,
    onSave: (Child) -> Unit
) {
    var fullName by remember { mutableStateOf(child.fullName) }
    var motherName by remember { mutableStateOf(child.motherName) }
    var phone by remember { mutableStateOf(child.phone) }
    var gender by remember { mutableStateOf(child.gender) }
    var healthCenter by remember { mutableStateOf(child.healthCenter) }
    var address by remember { mutableStateOf(child.address) }
    var dob by remember { mutableStateOf(child.dob) }
    var vaccinationNumber by remember { mutableStateOf(child.vaccinationNumber) }
    var familyNumber by remember { mutableStateOf(child.familyNumber) }
    var headOfHousehold by remember { mutableStateOf(child.headOfHousehold) }
    var isAdultTd by remember { mutableStateOf(child.isAdultTd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الطفل") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("اسم الطفل الرباعي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("اسم الأم الثلاثي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = healthCenter,
                    onValueChange = { healthCenter = it },
                    label = { Text("أقرب مركز صحي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("عنوان السكن") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("تاريخ الميلاد (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vaccinationNumber,
                    onValueChange = { vaccinationNumber = it },
                    label = { Text("الرقم التلقيحي للطفل في السجل الدائمي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = familyNumber,
                    onValueChange = { familyNumber = it },
                    label = { Text("رقم الأسرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = headOfHousehold,
                    onValueChange = { headOfHousehold = it },
                    label = { Text("اسم رب الأسرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(checked = isAdultTd, onCheckedChange = { isAdultTd = it })
                    Text("لقاح ثنائي كبار")
                }
                
                Text("الجنس", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = gender == "ذكر", onClick = { gender = "ذكر" })
                        Text("ذكر")
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = gender == "أنثى", onClick = { gender = "أنثى" })
                        Text("أنثى")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(child.copy(
                    fullName = fullName,
                    motherName = motherName,
                    phone = phone,
                    gender = gender,
                    healthCenter = healthCenter,
                    address = address,
                    dob = dob,
                    vaccinationNumber = vaccinationNumber,
                    familyNumber = familyNumber,
                    headOfHousehold = headOfHousehold,
                    isAdultTd = isAdultTd
                ))
            }) {
                Text("حفظ التغييرات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
