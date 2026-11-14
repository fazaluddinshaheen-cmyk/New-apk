package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.WalletTransaction
import com.example.ui.theme.AmberPending
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

fun formatPkr(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
    return "Rs. ${formatter.format(amount.toLong())}"
}

fun formatTimestamp(timestamp: Timestamp?): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(date)
}

fun statusLabelUrdu(raw: String): String {
    val upper = raw.trim().uppercase()
    if (upper.startsWith("FACE:")) {
        val inner = upper.removePrefix("FACE:").trim()
        return "فیس: ${statusLabelUrdu(inner)}"
    }
    return when (upper) {
        "PENDING" -> "زیر غور"
        "APPROVED" -> "منظور شدہ"
        "REJECTED" -> "مسترد"
        "VERIFIED" -> "تصدیق شدہ"
        "UNVERIFIED" -> "غیر تصدیق شدہ"
        "ACTIVE" -> "فعال"
        "COMPLETED" -> "مکمل"
        "PAUSED" -> "رکا ہوا"
        else -> raw
    }
}

@Composable
fun statusColor(status: String): Color {
    val clean = status.substringAfter(":").trim().uppercase()
    return when (clean) {
        "APPROVED", "VERIFIED", "COMPLETED", "ACTIVE" -> MintProfit
        "PENDING" -> AmberPending
        "REJECTED" -> CoralAlert
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = statusColor(text),
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(50),
        modifier = modifier.border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Text(
            text = statusLabelUrdu(text),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BalanceHeroCard(
    balance: Double,
    totalInvested: Double,
    totalEarnings: Double,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_wealth_hero),
            contentDescription = "Portfolio vault background",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دستیاب والٹ بیلنس",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MintProfit.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MintProfit)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "لائیو",
                            style = MaterialTheme.typography.labelSmall,
                            color = MintProfit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatPkr(balance),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("live_wallet_balance")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricMiniBox(
                    label = "کل سرمایہ کاری",
                    value = formatPkr(totalInvested),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniBox(
                    label = "کل منافع",
                    value = formatPkr(totalEarnings),
                    valueColor = MintProfit,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDepositClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_deposit_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "ڈپازٹ",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ڈپازٹ", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onWithdrawClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_withdraw_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "نکالیں",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("نکالیں", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MetricMiniBox(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            RoundedCornerShape(12.dp)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                color = valueColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: WalletTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.type in listOf("DEPOSIT", "DAILY_PROFIT", "REFUND", "REFERRAL_BONUS", "PROMO_BONUS")
    val sign = if (isCredit) "+" else "-"
    val amountColor = when {
        transaction.status == "REJECTED" -> MaterialTheme.colorScheme.onSurfaceVariant
        isCredit -> MintProfit
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("tx_card_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor(transaction.status).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        "DAILY_PROFIT", "REFERRAL_BONUS", "PROMO_BONUS" -> Icons.AutoMirrored.Filled.TrendingUp
                        "INVESTMENT" -> Icons.Default.Shield
                        else -> Icons.AutoMirrored.Filled.ReceiptLong
                    },
                    contentDescription = transaction.title,
                    tint = statusColor(transaction.status)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTimestamp(transaction.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sign${formatPkr(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(text = transaction.status)
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: WalletTransaction,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لین دین کی رسید",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "رسید بند کریں")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(text = transaction.status)
                Text(
                    text = formatPkr(transaction.amount),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MonoFontFamily),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ReceiptDetailRow("عنوان", transaction.title)
                ReceiptDetailRow(
                    "قسم",
                    when (transaction.type.uppercase()) {
                        "DEPOSIT" -> "ڈپازٹ"
                        "WITHDRAWAL" -> "نکالنا"
                        "INVESTMENT" -> "سرمایہ کاری"
                        "DAILY_PROFIT" -> "روزانہ منافع"
                        "REFUND" -> "واپسی رقم"
                        "REFERRAL_BONUS" -> "ریفرل بونس"
                        "PROMO_BONUS" -> "پرومو بونس"
                        else -> transaction.type.replace("_", " ")
                    }
                )
                ReceiptDetailRow("ذریعہ", transaction.method)
                if (transaction.fee > 0) {
                    ReceiptDetailRow("ڈپازٹ فیس", formatPkr(transaction.fee))
                }
                ReceiptDetailRow("حوالہ نمبر", transaction.referenceId.ifBlank { transaction.id })
                ReceiptDetailRow("تفصیل", transaction.description)
                ReceiptDetailRow("وقت", formatTimestamp(transaction.createdAt))
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("بند کریں")
            }
        }
    )
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun TwoFactorPinDialog(
    title: String = "ٹو فیکٹر تصدیق",
    subtitle: String = "جاری رکھنے کے لیے اپنا 2FA سیکیورٹی پن درج کریں۔",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "2FA تصدیق",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("سیکیورٹی پن") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("two_factor_challenge_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length in 4..6,
                modifier = Modifier.testTag("two_factor_verify_button")
            ) {
                Text("پن کی تصدیق کریں")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("منسوخ کریں")
            }
        }
    )
}

@Composable
fun MaintenanceOverlayScreen(
    message: String,
    onSwitchAccountClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 460.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AmberPending.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "مرمت جاری ہے",
                        tint = AmberPending,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "سسٹم مرمت جاری ہے",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onSwitchAccountClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("maintenance_logout_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "لاگ ان پر واپس جائیں", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لاگ ان پورٹل پر واپس جائیں")
                }
            }
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = actionLabel,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
