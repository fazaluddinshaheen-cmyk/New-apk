package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.KycStatus
import com.example.data.model.UserProfile
import com.example.ui.components.ImageEncodingUtils
import com.example.ui.components.StatusPill
import com.example.ui.components.formatPkr
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.MintProfit
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.FaceAiState

@Composable
fun ProfileSecurityKycScreen(
    profile: UserProfile?,
    googleEmail: String,
    isBusy: Boolean,
    faceAiState: FaceAiState,
    onUpdateProfile: (fullName: String, phone: String) -> Unit,
    onUpdateAvatar: (avatarBase64: String) -> Unit,
    onSubmitAiFaceVerification: (faceBitmap: Bitmap) -> Unit,
    onUpdateTwoFactor: (enabled: Boolean, pin: String?) -> Unit,
    onOpenLiveChat: () -> Unit,
    onCopiedNotice: (String) -> Unit,
    onLockAppSession: () -> Unit,
    onGoogleSignOut: () -> Unit
) {
    val context = LocalContext.current

    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var phone by remember(profile?.phone) { mutableStateOf(profile?.phone ?: "") }

    var twoFactorSwitch by remember(profile?.twoFactorEnabled) { mutableStateOf(profile?.twoFactorEnabled ?: false) }
    var twoFactorPin by remember { mutableStateOf("") }

    val faceStatus = KycStatus.fromString(profile?.faceStatus ?: KycStatus.UNVERIFIED.name)
    val isAnalyzingFace = faceAiState is FaceAiState.Analyzing

    val avatarBitmap = remember(profile?.avatarBase64) {
        ImageEncodingUtils.decodeBase64ToImageBitmap(profile?.avatarBase64)
    }
    val faceSelfieBitmap = remember(profile?.faceSelfieBase64) {
        ImageEncodingUtils.decodeBase64ToImageBitmap(profile?.faceSelfieBase64)
    }

    // Zero-permission Android Photo Picker for Profile Photo
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageEncodingUtils.uriToCompactBase64(context, uri, maxDimension = 180, quality = 68)
            if (!encoded.isNullOrBlank()) {
                onUpdateAvatar(encoded)
            } else {
                onCopiedNotice("منتخب تصویر پر کارروائی نہیں ہو سکی۔")
            }
        }
    }

    // Live camera selfie capture via TakePicturePreview -> analyzed directly by Firebase AI Logic
    val faceSelfieCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onSubmitAiFaceVerification(bitmap)
        }
    }

    // Zero-permission Photo Picker for Face Verification -> analyzed directly by Firebase AI Logic
    val faceSelfieGalleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = ImageEncodingUtils.uriToScaledBitmap(context, uri, maxDimension = 512)
            if (bitmap != null) {
                onSubmitAiFaceVerification(bitmap)
            } else {
                onCopiedNotice("منتخب تصویر لوڈ نہیں ہو سکی۔")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header with Tap-to-Change Photo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable {
                                        avatarPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("upload_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarBitmap != null) {
                                    Image(
                                        bitmap = avatarBitmap,
                                        contentDescription = "پروفائل تصویر",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "پروفائل تصویر لگائیں",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.fullName?.ifBlank { "سرمایہ کار پروفائل" } ?: "سرمایہ کار پروفائل",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${profile?.username ?: "investor"} • ${googleEmail.ifBlank { profile?.email ?: "" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusPill(text = formatPkr(profile?.balance ?: 0.0))
                                    StatusPill(text = "FACE: ${faceStatus.name}")
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                avatarPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isBusy && !isAnalyzingFace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("change_profile_photo_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "پروفائل تصویر منتخب کریں", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (avatarBitmap != null) "پروفائل تصویر تبدیل کریں" else "پروفائل تصویر لگائیں")
                        }
                    }
                }
            }

            // AI-Powered Face Verification Card (ONLY verification method — CNIC completely removed)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "AI فیس ویریفکیشن",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AI فیس ویریفکیشن",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI سسٹم",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "صرف AI چہرے کی تصدیق • شناختی کارڈ کی ضرورت نہیں",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            StatusPill(text = faceStatus.name)
                        }

                        if (isAnalyzingFace) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "AI آپ کے چہرے کی تصدیق اور معیار چیک کر رہا ہے...",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                }
                            }
                        }

                        if (faceAiState is FaceAiState.Error) {
                            Surface(
                                color = CoralAlert.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = faceAiState.message,
                                    color = CoralAlert,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (faceSelfieBitmap != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    bitmap = faceSelfieBitmap,
                                    contentDescription = "فیس ویریفکیشن سیلفی",
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = when (faceStatus) {
                                            KycStatus.VERIFIED -> "AI کے ذریعے آپ کے چہرے کی تصدیق مکمل ہو چکی ہے!"
                                            KycStatus.PENDING -> "آپ کی سیلفی تصدیق کے مرحلے میں ہے۔"
                                            KycStatus.REJECTED -> "AI چہرے کی تصدیق مسترد ہوئی — براہ کرم روشن جگہ سے واضح سیلفی لیں۔"
                                            else -> "AI فیس تصدیق کے لیے تیار ہے۔"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (faceStatus == KycStatus.VERIFIED) MintProfit else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!profile?.faceAiNote.isNullOrBlank()) {
                                        Text(
                                            text = profile?.faceAiNote ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (faceStatus == KycStatus.REJECTED) CoralAlert else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "اپنے اکاؤنٹ کی حفاظت کے لیے کیمرے سے لائیو سیلفی لیں یا گیلری سے واضح چہرے کی تصویر منتخب کریں۔ AI خودکار طور پر آپ کے چہرے کی تصدیق کرے گا۔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (faceStatus != KycStatus.VERIFIED) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { faceSelfieCameraLauncher.launch(null) },
                                    enabled = !isBusy && !isAnalyzingFace,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("capture_face_selfie_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = "لائیو سیلفی لیں", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("سیلفی لیں", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        faceSelfieGalleryPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    enabled = !isBusy && !isAnalyzingFace,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pick_face_selfie_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = "گیلری سے منتخب کریں", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("گیلری")
                                }
                            }
                        }
                    }
                }
            }

            // Invite Code & Live Support Quick Card
            item {
                val referralCode = profile?.referralCode?.ifBlank { "CW000000" } ?: "CW000000"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = "دعوتی کوڈ", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("دعوتی کوڈ: $referralCode", style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily), fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Invite Code", referralCode))
                                    onCopiedNotice("دعوتی کوڈ $referralCode کاپی ہو گیا!")
                                },
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "کاپی", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("کاپی")
                            }
                        }
                        Button(
                            onClick = onOpenLiveChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_live_chat_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "ایڈمن سے لائیو چیٹ", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ایڈمن سپورٹ سے لائیو چیٹ", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Profile Details Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "پروفائل کی تفصیلات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("مکمل نام") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "مکمل نام") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_fullname_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("موبائل نمبر") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "موبائل نمبر") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_phone_input")
                        )
                        Button(
                            onClick = { onUpdateProfile(fullName, phone) },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_profile_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("تبدیلیاں محفوظ کریں", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Two-Factor Authentication Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "ٹو فیکٹر سیکیورٹی",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ٹو فیکٹر سیکیورٹی (2FA)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "لاگ ان اور رقم نکالتے وقت سیکیورٹی پن لازمی قرار دیں",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = twoFactorSwitch,
                                onCheckedChange = { checked ->
                                    twoFactorSwitch = checked
                                    if (!checked) {
                                        onUpdateTwoFactor(false, null)
                                    }
                                },
                                modifier = Modifier.testTag("toggle_2fa_switch")
                            )
                        }

                        if (twoFactorSwitch) {
                            OutlinedTextField(
                                value = twoFactorPin,
                                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) twoFactorPin = it },
                                label = { Text("4 سے 6 ہندسوں کا 2FA سیکیورٹی پن") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "2FA پن") },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("two_factor_pin_input")
                            )
                            Button(
                                onClick = {
                                    onUpdateTwoFactor(true, twoFactorPin)
                                    twoFactorPin = ""
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_2fa_pin_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("2FA پن محفوظ / فعال کریں", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Session Controls
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onLockAppSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("app_logout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "اکاؤنٹ لاگ آؤٹ", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("لاگ ان پورٹل پر لاگ آؤٹ کریں")
                    }

                    Button(
                        onClick = onGoogleSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_out_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "گوگل سیشن ختم کریں", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google سیشن منقطع کریں", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
