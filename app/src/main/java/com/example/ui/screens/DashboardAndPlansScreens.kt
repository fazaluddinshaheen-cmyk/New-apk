package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.KycStatus
import com.example.data.model.UserProfile
import com.example.data.model.VipInvestment
import com.example.data.model.VipPlan
import com.example.data.model.VipPlanCatalog
import com.example.data.model.WalletTransaction
import com.example.ui.components.BalanceHeroCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ImageEncodingUtils
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StatusPill
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionRowCard
import com.example.ui.components.formatPkr
import com.example.ui.theme.AmberPending
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily

@Composable
fun DashboardScreen(
    profile: UserProfile?,
    investments: List<VipInvestment>,
    transactions: List<WalletTransaction>,
    referralBonusAmount: Double,
    unreadSupportCount: Int,
    isBusy: Boolean,
    onNavigateDeposit: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateProfile: () -> Unit,
    onOpenLiveChat: () -> Unit,
    onRedeemPromoCode: (String) -> Unit,
    onClaimProfit: (VipInvestment) -> Unit,
    onCopiedNotice: (String) -> Unit,
    onLockSession: () -> Unit
) {
    val context = LocalContext.current
    var selectedTx by remember { mutableStateOf<WalletTransaction?>(null) }
    var promoInput by remember { mutableStateOf("") }
    val avatarBitmap = remember(profile?.avatarBase64) {
        ImageEncodingUtils.decodeBase64ToImageBitmap(profile?.avatarBase64)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable(onClick = onNavigateProfile),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome To App",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "خوش آمدید، ${profile?.fullName?.ifBlank { profile.username } ?: "سرمایہ کار"}",
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = "AI فیس: ${KycStatus.fromString(profile?.faceStatus ?: "UNVERIFIED").label}",
                                    color = if (profile?.faceStatus == KycStatus.VERIFIED.name) MintProfit else AmberPending
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onLockSession,
                        modifier = Modifier.testTag("dashboard_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "سیشن لاک کریں",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                BalanceHeroCard(
                    balance = profile?.balance ?: 0.0,
                    totalInvested = profile?.totalInvested ?: 0.0,
                    totalEarnings = profile?.totalEarnings ?: 0.0,
                    onDepositClick = onNavigateDeposit,
                    onWithdrawClick = onNavigateWithdraw
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.Diamond,
                        label = "VIP پلانز",
                        onClick = onNavigatePlans,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_plans_tile")
                    )
                    QuickActionTile(
                        icon = Icons.Default.Face,
                        label = "AI فیس تصدیق",
                        onClick = onNavigateProfile,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_face_tile")
                    )
                    QuickActionTile(
                        icon = Icons.Default.SupportAgent,
                        label = "لائیو چیٹ",
                        badgeCount = unreadSupportCount,
                        onClick = onOpenLiveChat,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_live_chat_tile")
                    )
                }
            }

            // Invite Friends Card (Rs. 250 per qualifying deposit)
            item {
                val referralCode = profile?.referralCode?.ifBlank { "CW000000" } ?: "CW000000"
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
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = "دوستوں کو دعوت دیں",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "دوستوں کو بلائیں اور ${formatPkr(referralBonusAmount)} کمائیں",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill(
                                text = "${profile?.referralCount ?: 0} شامل ہوئے",
                                color = MintProfit
                            )
                        }
                        Text(
                            text = "اپنا دعوتی کوڈ شیئر کریں۔ جب آپ کا دوست پہلا منظور شدہ ڈپازٹ مکمل کرے گا تو آپ کو خودکار ${formatPkr(referralBonusAmount)} ملیں گے۔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = "آپ کا دعوتی کوڈ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = referralCode,
                                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Invite Code", referralCode))
                                    onCopiedNotice("دعوتی کوڈ $referralCode کاپی ہو گیا!")
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("copy_referral_code_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "کوڈ کاپی کریں", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("کاپی")
                            }
                        }
                        if ((profile?.referralEarnings ?: 0.0) > 0.0) {
                            Text(
                                text = "کل ریفرل کمائی: ${formatPkr(profile?.referralEarnings ?: 0.0)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MintProfit
                            )
                        }
                    }
                }
            }

            // Promo Code Redemption Card (Active VIP Plan Exclusive)
            item {
                val hasActivePlan = (profile?.hasActivePlan == true) || investments.isNotEmpty()
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
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = "پرومو کوڈ",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VIP پرومو کوڈ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill(
                                text = if (hasActivePlan) "ان لاک شدہ" else "صرف فعال پلان",
                                color = if (hasActivePlan) MintProfit else AmberPending
                            )
                        }

                        if (!hasActivePlan) {
                            Text(
                                text = "پرومو کوڈ صرف فعال VIP سرمایہ کار استعمال کر سکتے ہیں۔ پرومو انعام کھولنے کے لیے کوئی VIP پلان فعال کریں۔",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberPending
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = promoInput,
                                onValueChange = { promoInput = it.uppercase() },
                                label = { Text("پرومو کوڈ درج کریں") },
                                singleLine = true,
                                enabled = hasActivePlan && !isBusy,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("promo_code_input")
                            )
                            Button(
                                onClick = {
                                    onRedeemPromoCode(promoInput)
                                    promoInput = ""
                                },
                                enabled = hasActivePlan && promoInput.trim().length >= 3 && !isBusy,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("redeem_promo_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حاصل کریں", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderRow(
                    title = "فعال VIP سرمایہ کاری",
                    actionLabel = "تمام پلانز",
                    onActionClick = onNavigatePlans
                )
            }

            if (investments.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "ابھی کوئی فعال VIP پلان نہیں",
                        subtitle = "ایزی پیسہ یا جاز کیش سے ڈپازٹ کریں اور روزانہ Rs. 330 کمانے کے لیے VIP پلان 1 (Rs. 550) فعال کریں۔"
                    )
                }
            } else {
                items(investments, key = { it.id }) { investment ->
                    ActiveInvestmentCard(
                        investment = investment,
                        isBusy = isBusy,
                        onClaimProfit = { onClaimProfit(investment) }
                    )
                }
            }

            item {
                SectionHeaderRow(
                    title = "حالیہ لین دین",
                    actionLabel = "سب دیکھیں",
                    onActionClick = onNavigateHistory
                )
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "ابھی کوئی لین دین نہیں",
                        subtitle = "آپ کے ڈپازٹس، VIP پلانز، روزانہ منافع، ریفرل انعامات اور نکالی گئی رقوم یہاں ظاہر ہوں گی۔"
                    )
                }
            } else {
                items(transactions.take(4), key = { it.id }) { tx ->
                    TransactionRowCard(
                        transaction = tx,
                        onClick = { selectedTx = tx }
                    )
                }
            }
        }
    }

    if (selectedTx != null) {
        TransactionDetailDialog(
            transaction = selectedTx!!,
            onDismiss = { selectedTx = null }
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    badgeCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge { Text(badgeCount.toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActiveInvestmentCard(
    investment: VipInvestment,
    isBusy: Boolean,
    onClaimProfit: () -> Unit
) {
    val progress = if (investment.durationDays > 0) {
        investment.daysClaimed.toFloat() / investment.durationDays.toFloat()
    } else 0f
    val isActive = investment.status == "ACTIVE" && investment.daysClaimed < investment.durationDays

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
                Column {
                    Text(
                        text = investment.planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "سرمایہ کاری ${formatPkr(investment.investedAmount)} • روزانہ ${formatPkr(investment.dailyReturn)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(text = investment.status)
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.background
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دن ${investment.daysClaimed} / ${investment.durationDays} • کمائی ${formatPkr(investment.totalEarned)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintProfit
                )
                if (isActive) {
                    Button(
                        onClick = onClaimProfit,
                        enabled = !isBusy,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("claim_profit_${investment.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "روزانہ منافع حاصل کریں",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("وصول +${formatPkr(investment.dailyReturn)}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VipPlansScreen(
    walletBalance: Double,
    isBusy: Boolean,
    onInvestPlan: (VipPlan) -> Unit,
    onNavigateDeposit: () -> Unit
) {
    var confirmPlan by remember { mutableStateOf<VipPlan?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VIP سرمایہ کاری پلانز",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "روزانہ منافع براہِ راست آپ کے والٹ میں جمع ہوتا ہے",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable(onClick = onNavigateDeposit)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "والٹ بیلنس",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatPkr(walletBalance),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = MonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            items(VipPlanCatalog.plans, key = { it.id }) { plan ->
                VipPlanCard(
                    plan = plan,
                    walletBalance = walletBalance,
                    isBusy = isBusy,
                    onActivateClick = { confirmPlan = plan }
                )
            }
        }
    }

    if (confirmPlan != null) {
        val plan = confirmPlan!!
        val hasBalance = walletBalance >= plan.investmentAmount
        AlertDialog(
            onDismissRequest = { confirmPlan = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("${plan.name} فعال کریں") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "سرمایہ کاری: ${formatPkr(plan.investmentAmount)}\nروزانہ منافع: ${formatPkr(plan.dailyReturn)}\nمدت: ${plan.durationDays} دن\nکل واپسی: ${formatPkr(plan.totalReturn)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!hasBalance) {
                        Text(
                            text = "آپ کا بیلنس (${formatPkr(walletBalance)}) ناکافی ہے۔ پہلے ایزی پیسہ یا جاز کیش سے ڈپازٹ کریں۔",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (hasBalance) {
                    Button(
                        onClick = {
                            onInvestPlan(plan)
                            confirmPlan = null
                        },
                        modifier = Modifier.testTag("confirm_invest_${plan.id}")
                    ) {
                        Text("سرمایہ کاری کی تصدیق کریں")
                    }
                } else {
                    Button(
                        onClick = {
                            confirmPlan = null
                            onNavigateDeposit()
                        }
                    ) {
                        Text("رقم ڈپازٹ کریں")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmPlan = null }) {
                    Text("منسوخ کریں")
                }
            }
        )
    }
}

@Composable
private fun VipPlanCard(
    plan: VipPlan,
    walletBalance: Double,
    isBusy: Boolean,
    onActivateClick: () -> Unit
) {
    val borderColor = if (plan.isMega) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (plan.isMega) 1.5.dp else 1.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (plan.isMega) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${plan.durationDays} دن کا دورانیہ • کل ادائیگی ${formatPkr(plan.totalReturn)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = plan.badge,
                    color = if (plan.isMega) MaterialTheme.colorScheme.primary else MintProfit
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlanStatBox(
                    label = "سرمایہ کاری",
                    value = formatPkr(plan.investmentAmount),
                    modifier = Modifier.weight(1f)
                )
                PlanStatBox(
                    label = "روزانہ منافع",
                    value = formatPkr(plan.dailyReturn),
                    highlight = true,
                    modifier = Modifier.weight(1f)
                )
                PlanStatBox(
                    label = "مدت",
                    value = "${plan.durationDays} دن",
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onActivateClick,
                enabled = !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("invest_plan_${plan.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (plan.isMega) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = "${plan.name} میں سرمایہ کاری کریں",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (walletBalance >= plan.investmentAmount) {
                        "سرمایہ کاری کریں (${formatPkr(plan.investmentAmount)})"
                    } else {
                        "پلان فعال کریں (${formatPkr(plan.investmentAmount)})"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlanStatBox(
    label: String,
    value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                color = if (highlight) MintProfit else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
