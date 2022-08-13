import dev.inmo.micro_utils.coroutines.subscribeSafelyWithoutExceptions
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.*
import dev.inmo.tgbotapi.extensions.api.send.media.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.CommonMessageFilterExcludeMediaGroups
import dev.inmo.tgbotapi.extensions.behaviour_builder.filters.MessageFilterByChat
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.shortcuts.*
import kotlinx.coroutines.*

suspend fun activateResenderBot(
    token: String,
    print: (Any) -> Unit
) {
    val bot = telegramBot(token)

    print(bot.getMe())

    bot.buildBehaviourWithLongPolling(CoroutineScope(currentCoroutineContext() + SupervisorJob())) {
        onContentMessage(
            initialFilter = CommonMessageFilterExcludeMediaGroups,
            subcontextUpdatesFilter = MessageFilterByChat
        ) {
            val chat = it.chat
            withTypingAction(chat) {
                executeUnsafe(it.content.createResend(chat.id, replyToMessageId = it.messageId)) {
                    it.forEach(print)
                }
            }
        }
        onVisualGallery {
            val chat = it.chat ?: return@onVisualGallery
            withUploadPhotoAction(chat) {
                send(chat, it.map { it.content.toMediaGroupMemberTelegramMedia() })
            }
        }
        onPlaylist {
            val chat = it.chat ?: return@onPlaylist
            withUploadDocumentAction(chat) {
                send(chat, it.map { it.content.toMediaGroupMemberTelegramMedia() })
            }
        }
        onDocumentsGroup {
            val chat = it.chat ?: return@onDocumentsGroup
            withUploadDocumentAction(chat) {
                send(chat, it.map { it.content.toMediaGroupMemberTelegramMedia() })
            }
        }

        allUpdatesFlow.subscribeSafelyWithoutExceptions(this) {
            println(it)
        }
    }.join()
}
