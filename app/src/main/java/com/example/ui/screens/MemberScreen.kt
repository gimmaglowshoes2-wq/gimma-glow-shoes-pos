package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerMember
import com.example.printer.ThermalReceiptHelper
import com.example.ui.viewmodel.ShoePosViewModel

@Composable
fun MemberScreen(
    viewModel: ShoePosViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allMembers by viewModel.allMembers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    var editingMember by remember { mutableStateOf<CustomerMember?>(null) }
    var isNewMemberDialog by remember { mutableStateOf(false) }

    val filteredMembers = remember(allMembers, searchQuery) {
        if (searchQuery.isBlank()) allMembers
        else allMembers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Cari Member (Nama / No WA)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Loyalty Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sistem Loyalitas Pelanggan",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Member otomatis dapat poin bonus 5% dari tiap cuci sepatu. Poin bisa dipakai sebagai diskon di nota berikutnya!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member list
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada member terdaftar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.id }) { member ->
                        MemberItemCard(
                            member = member,
                            onEdit = { editingMember = member },
                            onDelete = { viewModel.deleteMember(member.id) },
                            onWhatsAppClick = {
                                var phone = member.phone.replace(Regex("[^0-9]"), "")
                                if (phone.startsWith("0")) phone = "62" + phone.substring(1)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button to Add Member
        FloatingActionButton(
            onClick = {
                isNewMemberDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_member_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Member")
        }
    }

    // Modal: Add / Edit Member Dialog
    if (isNewMemberDialog || editingMember != null) {
        val target = editingMember ?: CustomerMember(name = "", phone = "")
        var nameInput by remember { mutableStateOf(target.name) }
        var phoneInput by remember { mutableStateOf(target.phone) }
        var notesInput by remember { mutableStateOf(target.notes) }
        var pointsInput by remember { mutableStateOf(target.totalPoints.toString()) }

        AlertDialog(
            onDismissRequest = {
                isNewMemberDialog = false
                editingMember = null
            },
            title = {
                Text(
                    text = if (editingMember != null) "Edit Data Member" else "Daftar Member Baru",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("member_name_input")
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Nomor WhatsApp") },
                        placeholder = { Text("081234567890") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("member_phone_input")
                    )
                    OutlinedTextField(
                        value = pointsInput,
                        onValueChange = { pointsInput = it },
                        label = { Text("Jumlah Poin Loyalitas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Catatan / Sepatu Favorit") },
                        placeholder = { Text("Misal: Suka cuci Air Jordan, ukuran 42") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val pts = pointsInput.toIntOrNull() ?: target.totalPoints
                            val updated = target.copy(
                                name = nameInput.trim(),
                                phone = phoneInput.trim(),
                                notes = notesInput.trim(),
                                totalPoints = pts
                            )
                            viewModel.saveMember(updated)
                            isNewMemberDialog = false
                            editingMember = null
                        }
                    },
                    modifier = Modifier.testTag("save_member_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        isNewMemberDialog = false
                        editingMember = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MemberItemCard(
    member: CustomerMember,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    val tierColor = when (member.tier.lowercase()) {
        "platinum" -> Color(0xFF8B5CF6)
        "gold" -> Color(0xFFF59E0B)
        else -> Color(0xFF64748B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "WA: ${member.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = tierColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${member.tier} ⭐",
                        color = tierColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Poin: ${member.totalPoints} pts",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "${member.totalWashCount}x Cuci • Total ${ThermalReceiptHelper.formatRupiah(member.totalSpent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (member.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${member.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.phone.isNotBlank()) {
                    IconButton(onClick = onWhatsAppClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color(0xFF10B981)
                        )
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
