import dev.inmo.micro_utils.coroutines.AccumulatorFlow
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.*
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.*
import dev.inmo.tgbotapi.extensions.utils.extensions.parseCommandsWithParams
import dev.inmo.tgbotapi.extensions.utils.formatting.*
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.utils.botCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

sealed interface BotState : State
data class ExpectContentOrStopState(override val context: ChatId, val sourceMessage: CommonMessage<TextContent>) : BotState
data class StopState(override val context: ChatId) : BotState

suspend fun main(args: Array<String>) {
    val botToken = args.first()

    telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
        botToken,
        CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
            when (state) {
                is ExpectContentOrStopState -> {
                    println("Thrown error on ExpectContentOrStopState")
                }
                is StopState -> {
                    println("Thrown error on StopState")
                }
            }
            e.printStackTrace()
            state
        }
    ) {
        strictlyOn<ExpectContentOrStopState> {
            sendMessage(
                it.context,
                buildEntities {
                    +"Send me some content or " + botCommand("stop") + " if you want to stop sending"
                }
            )

            val contentMessage = waitContentMessage().first()
            val content = contentMessage.content

            when {
                content is TextContent && content.parseCommandsWithParams().keys.contains("stop") -> StopState(it.context)
                else -> {
                    execute(content.createResend(it.context))
                    it
                }
            }
        }
        strictlyOn<StopState> {
            send(it.context, "You have stopped sending of content")

            null
        }

        command("start") {
            startChain(ExpectContentOrStopState(it.chat.id, it))
        }
    }.second.join()
}
