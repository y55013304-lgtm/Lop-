package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ThreatAlert

data class PendingDownload(
    val url: String,
    val userAgent: String,
    val contentDisposition: String,
    val mimeType: String,
    val contentLength: Long,
    val fileName: String,
    val threatAlert: ThreatAlert? = null
)

@Composable
fun DownloadProtectionDialog(
    pendingDownload: PendingDownload,
    onConfirmDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onScanWithVirusTotal: (String) -> Unit
) {
    val isDangerous = pendingDownload.threatAlert != null
    var showAdvancedDanger by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelDownload,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDangerous) Color(0xFFD32F2F).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDangerous) Icons.Default.Dangerous else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (isDangerous) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = if (isDangerous) "危険なダウンロードを中断しました" else "ファイルのダウンロード",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDangerous) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Threat Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDangerous) Color(0xFFB71C1C).copy(alpha = 0.2f)
                    else Color(0xFF1B5E20).copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isDangerous) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDangerous) "VirusTotal / セーフガード: 高危険度ファイルを検知"
                            else "VirusTotal スキャン済み: 安全性が確認されました",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isDangerous) Color(0xFFFF8A80) else Color(0xFF81C784)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File Details Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "ファイル名:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pendingDownload.fileName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (isDangerous) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurface
                        )

                        if (pendingDownload.contentLength > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val sizeKb = pendingDownload.contentLength / 1024
                            val sizeStr = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else "$sizeKb KB"
                            Text(
                                text = "ファイルサイズ: $sizeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (pendingDownload.threatAlert != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "警告詳細: ${pendingDownload.threatAlert.detail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEF5350),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (isDangerous) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "外部APKや実行可能プログラムは、ウイルス感染やスマートフォンの不正操作につながる恐れがあります。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (isDangerous) {
                // If dangerous, primary confirm button is "ABORT" (Safe action)
                Button(
                    onClick = onCancelDownload,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ダウンロードを中止（推奨）", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConfirmDownload,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ダウンロード開始")
                }
            }
        },
        dismissButton = {
            if (isDangerous) {
                Column(horizontalAlignment = Alignment.End) {
                    OutlinedButton(
                        onClick = { onScanWithVirusTotal(pendingDownload.url) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("VirusTotalで調査", fontSize = 12.sp)
                    }

                    if (!showAdvancedDanger) {
                        TextButton(
                            onClick = { showAdvancedDanger = true }
                        ) {
                            Text(
                                "危険を理解して保存...",
                                color = Color(0xFFE57373),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onConfirmDownload
                        ) {
                            Text(
                                "危険を承知でダウンロード（非推奨）",
                                color = Color(0xFFFF1744),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                TextButton(onClick = onCancelDownload) {
                    Text("キャンセル")
                }
            }
        }
    )
}
