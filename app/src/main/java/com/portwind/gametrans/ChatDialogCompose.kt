package com.portwind.gametrans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onSendMessage: suspend (String, List<ChatMessage>) -> String?,
    coroutineScope: CoroutineScope
) {
    if (isVisible) {
        // Use a full-screen semi-transparent box to act as a scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, // no ripple
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            val chatMessages = remember { mutableStateListOf<ChatMessage>() }
            var input by remember { mutableStateOf("") }
            var sending by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }
            val clipboardManager = LocalClipboardManager.current
            val context = LocalContext.current

            // 自动请求焦点以弹出软键盘
            LaunchedEffect(Unit) {
                // 延迟一下确保组件完全渲染后再请求焦点
                kotlinx.coroutines.delay(100)
                focusRequester.requestFocus()
            }

            // 加载本地历史
            LaunchedEffect(Unit) {
                val history = loadChatHistory(context)
                if (history.isNotEmpty()) {
                    chatMessages.clear()
                    chatMessages.addAll(history)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume clicks to prevent closing dialog when clicking on it
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("问答对话", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        reverseLayout = true // 最新消息在底部
                    ) {
                        items(chatMessages.reversed()) { msg ->
                            val isAssistant = msg.role.equals("assistant", true)
                            
                            // 消息气泡样式
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAssistant) Arrangement.Start else Arrangement.End
                            ) {
                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 280.dp)
                                        .padding(vertical = 2.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(msg.text))
                                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                            }
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAssistant) 
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        else 
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isAssistant) 4.dp else 12.dp,
                                        bottomEnd = if (isAssistant) 12.dp else 4.dp
                                    )
                                ) {
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAssistant) 
                                            MaterialTheme.colorScheme.onSurface 
                                        else 
                                            MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 36.dp)
                            .focusRequester(focusRequester),
                        singleLine = false,
                        placeholder = { Text("请输入你的问题…") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val text = input.trim()
                                if (text.isNotEmpty() && !sending) {
                                    // 触发发送逻辑
                                    val currentHistory = chatMessages.toList()
                                    chatMessages.add(ChatMessage("user", text))
                                    input = ""
                                    sending = true
                                    saveChatHistory(context, chatMessages)
                                    
                                    coroutineScope.launch {
                                        try {
                                            val reply = onSendMessage(text, currentHistory)
                                            chatMessages.add(ChatMessage("assistant", reply ?: "(无回复)"))
                                            saveChatHistory(context, chatMessages)
                                        } catch (e: Exception) {
                                            chatMessages.add(ChatMessage("assistant", "(发送失败: ${e.message})"))
                                            saveChatHistory(context, chatMessages)
                                        } finally {
                                            sending = false
                                        }
                                    }
                                }
                            }
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(enabled = !sending, onClick = onDismissRequest) { Text("关闭") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            enabled = !sending && input.trim().isNotEmpty(),
                            onClick = {
                                val text = input.trim()
                                if (text.isEmpty()) return@Button
                                
                                // 先获取当前历史（不包括即将发送的消息）
                                val currentHistory = chatMessages.toList()
                                
                                // 添加用户消息到界面
                                chatMessages.add(ChatMessage("user", text))
                                input = ""
                                sending = true
                                saveChatHistory(context, chatMessages)
                                
                                coroutineScope.launch {
                                    try {
                                        // 传递历史记录和当前消息给API
                                        val reply = onSendMessage(text, currentHistory)
                                        chatMessages.add(ChatMessage("assistant", reply ?: "(无回复)"))
                                        saveChatHistory(context, chatMessages)
                                    } catch (e: Exception) {
                                        chatMessages.add(ChatMessage("assistant", "(发送失败: ${e.message})"))
                                        saveChatHistory(context, chatMessages)
                                    } finally {
                                        sending = false
                                    }
                                }
                            }
                        ) {
                            Text(if (sending) "发送中…" else "发送")
                        }
                    }
                }
            }
        }
    }
}

private const val CHAT_HISTORY_PREFS = "gametrans_chat"
private const val CHAT_HISTORY_KEY = "chat_history"

private fun loadChatHistory(context: android.content.Context): List<ChatMessage> {
    val prefs = context.getSharedPreferences(CHAT_HISTORY_PREFS, android.content.Context.MODE_PRIVATE)
    val json = prefs.getString(CHAT_HISTORY_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val role = obj.optString("role")
            val text = obj.optString("text")
            if (role.isNotBlank() && text.isNotBlank()) {
                list.add(ChatMessage(role, text))
            }
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveChatHistory(context: android.content.Context, messages: List<ChatMessage>) {
    val arr = JSONArray()
    messages.forEach { msg ->
        val obj = JSONObject()
        obj.put("role", msg.role)
        obj.put("text", msg.text)
        arr.put(obj)
    }
    val prefs = context.getSharedPreferences(CHAT_HISTORY_PREFS, android.content.Context.MODE_PRIVATE)
    prefs.edit().putString(CHAT_HISTORY_KEY, arr.toString()).apply()
}
