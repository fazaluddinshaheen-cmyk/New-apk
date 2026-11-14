package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.PromoCode
import com.example.data.model.SupportMessage
import com.example.data.model.SupportThread
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageEncodingUtils
import com.example.ui.components.StatusPill
import com.example.ui.components.formatPkr
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily

@Composable
fun AdminFaceReviewCard(
    user: UserProfile,
    isBusy: Boolean,
    onReviewFace: (userId: String, approve: Boolean) -> Unit
) {
    val selfieBitmap = remember(user.faceSelfieBase64) {
        ImageEncodingUtils.decodeBase64ToImageBitmap(user.faceSelfieBase64)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Face, contentDescription = "AI فیس ویریفکیشن", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("@${user.username} • ${user.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                StatusPill(text = user.faceStatus)
            }

            if (!user.faceAiNote.isNullOrBlank()) {
                Text(
                    text = user.faceAiNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (selfieBitmap != null) {
                Image(
                    bitmap = selfieBitmap,
                    contentDescription = "صارف کی فیس ویریفکیشن سیلفی",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onReviewFace(user.userId, true) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("approve_face_${user.userId}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MintProfit, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("فیس منظور کریں", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onReviewFace(user.userId, false) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reject_face_${user.userId}")
                ) {
                    Text("مسترد کریں", color = CoralAlert)
                }
            }
        }
    }
}

@Composable
fun AdminPromoCodesTab(
    promoCodes: List<PromoCode>,
    isBusy: Boolean,
    onCreatePromoCode: (code: String, bonus: Double, maxUses: Int, description: String) -> Unit,
    onTogglePromoActive: (code: String, active: Boolean) -> Unit,
    onDeletePromoCode: (code: String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var bonusInput by remember { mutableStateOf("200") }
    var maxUsesInput by remember { mutableStateOf("50") }
    var descInput by remember { mutableStateOf("فعال VIP پلان بونس") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 720.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = "پرومو کوڈ بنائیں", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("پرومو کوڈ جاری کریں (صرف فعال VIP کے لیے)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' } },
                        label = { Text("پرومو کوڈ (مثلاً VIP200)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_promo_code_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = bonusInput,
                            onValueChange = { bonusInput = it.filter { c -> c.isDigit() } },
                            label = { Text("بونس (Rs.)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("admin_promo_bonus_input")
                        )
                        OutlinedTextField(
                            value = maxUsesInput,
                            onValueChange = { maxUsesInput = it.filter { c -> c.isDigit() } },
                            label = { Text("زیادہ سے زیادہ استعمال") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("admin_promo_uses_input")
                        )
                    }
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("تفصیل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val bonus = bonusInput.toDoubleOrNull() ?: 0.0
                            val maxUses = maxUsesInput.toIntOrNull() ?: 50
                            onCreatePromoCode(codeInput, bonus, maxUses, descInput)
                            codeInput = ""
                        },
                        enabled = !isBusy && codeInput.length >= 3,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_create_promo_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "پرومو کوڈ بنائیں", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("پرومو کوڈ بنائیں", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (promoCodes.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "کوئی پرومو کوڈ موجود نہیں",
                    subtitle = "اوپر سے نیا پرومو کوڈ بنائیں۔ صرف فعال VIP پلان والے صارفین پرومو کوڈ استعمال کر سکتے ہیں۔"
                )
            }
        } else {
            items(promoCodes, key = { it.code }) { promo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = promo.code,
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPill(
                                    text = if (promo.isActive) "ACTIVE" else "PAUSED",
                                    color = if (promo.isActive) MintProfit else CoralAlert
                                )
                            }
                            Text(
                                text = "بونس ${formatPkr(promo.bonusAmount)} • استعمال ${promo.usedCount}/${promo.maxUses}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MintProfit
                            )
                            Text(
                                text = promo.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = promo.isActive,
                                onCheckedChange = { onTogglePromoActive(promo.code, it) }
                            )
                            IconButton(onClick = { onDeletePromoCode(promo.code) }) {
                                Icon(Icons.Default.Delete, contentDescription = "پرومو کوڈ حذف کریں", tint = CoralAlert)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLiveChatTab(
    threads: List<SupportThread>,
    selectedUserId: String?,
    messages: List<SupportMessage>,
    onSelectThread: (String?) -> Unit,
    onSendAdminReply: (targetUserId: String, targetUserName: String, text: String) -> Unit
) {
    val activeThread = remember(threads, selectedUserId) {
        threads.firstOrNull { it.userId == selectedUserId }
    }

    if (selectedUserId != null && activeThread != null) {
        LiveChatConversationPane(
            headerTitle = activeThread.userName,
            headerSubtitle = "Admin کے طور پر جواب • لائیو سپورٹ",
            messages = messages,
            viewerIsAdmin = true,
            onBack = { onSelectThread(null) },
            onSendMessage = { text ->
                onSendAdminReply(activeThread.userId, activeThread.userName, text)
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("سرمایہ کار لائیو چیٹ ان باکس", style = MaterialTheme.typography.titleLarge)
            }

            if (threads.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "ابھی کوئی لائیو چیٹ پیغام نہیں",
                        subtitle = "سرمایہ کاروں کے پیغامات یہاں آئیں گے۔ آپ کا جواب ہمیشہ Admin نام سے ظاہر ہوگا۔"
                    )
                }
            } else {
                items(threads, key = { it.userId }) { thread ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectThread(thread.userId) }
                            .testTag("admin_chat_thread_${thread.userId}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = thread.userName,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(thread.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = thread.lastMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = formatTimestamp(thread.updatedAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                            if (thread.unreadByAdmin > 0) {
                                Badge { Text(thread.unreadByAdmin.toString()) }
                            }
                        }
                    }
                }
            }
        }
    }
}
